import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, configurarCliente, peticion } from './cliente'

/*
 El cliente HTTP es el unico lugar por donde pasan TODAS las llamadas a la API, asi que un error aca rompe todas las pantallas.
 No se prueba contra un servidor: fetch se reemplaza por una funcion falsa que devuelve la respuesta que cada test necesita.
 Tampoco hace falta un navegador simulado: el cliente solo usa window.location.origin, que se reemplaza a mano
 */

const fetchFalso = vi.fn()

// respuesta de la API con el formato de error del backend: {status, error, message}
function respuestaJson(cuerpo: unknown, status = 200): Response {
  return new Response(JSON.stringify(cuerpo), { status, headers: { 'Content-Type': 'application/json' } })
}

beforeEach(() => {
  fetchFalso.mockReset()
  vi.stubGlobal('fetch', fetchFalso)
  vi.stubGlobal('window', { location: { origin: 'http://localhost:5174' } })
  configurarCliente({ alNoAutorizado: () => {} })
})

afterEach(() => {
  vi.unstubAllGlobals()
})

// espera que la llamada FALLE y devuelve el error (si no falla, el test falla). Con .catch(e => e) el error quedaria de tipo "unknown"
async function capturarError(llamada: Promise<unknown>): Promise<ApiError> {
  try {
    await llamada
  } catch (error) {
    return error as ApiError
  }
  throw new Error('Se esperaba que la llamada fallara, pero salio bien')
}

// los argumentos con los que se llamo a fetch: [url, opciones]
function ultimaLlamada(): [string, RequestInit] {
  return fetchFalso.mock.calls[fetchFalso.mock.calls.length - 1] as [string, RequestInit]
}

describe('armado del pedido', () => {
  it('manda la cookie de sesion y no pone Content-Type cuando no hay cuerpo', async () => {
    fetchFalso.mockResolvedValue(respuestaJson([]))

    await peticion('GET', '/api/pacientes')

    const [url, opciones] = ultimaLlamada()
    expect(url).toBe('/api/pacientes')
    expect(opciones.method).toBe('GET')
    expect(opciones.credentials).toBe('include')
    expect(opciones.headers).toEqual({})
    expect(opciones.body).toBeUndefined()
  })

  it('convierte el cuerpo a JSON y avisa el tipo con Content-Type', async () => {
    fetchFalso.mockResolvedValue(respuestaJson({ idPaciente: 1 }, 201))

    await peticion('POST', '/api/pacientes', { cuerpo: { nombre: 'Ana' } })

    const [, opciones] = ultimaLlamada()
    expect(opciones.headers).toEqual({ 'Content-Type': 'application/json' })
    expect(opciones.body).toBe('{"nombre":"Ana"}')
  })

  it('agrega los parametros a la URL y se saltea los vacios', async () => {
    fetchFalso.mockResolvedValue(respuestaJson([]))

    await peticion('GET', '/api/turnos', { params: { desde: '2031-03-03', hasta: '2031-03-07', busqueda: '', otro: null, nada: undefined } })

    expect(ultimaLlamada()[0]).toBe('/api/turnos?desde=2031-03-03&hasta=2031-03-07')
  })

  it('un numero cuenta como parametro (incluido el cero)', async () => {
    fetchFalso.mockResolvedValue(respuestaJson([]))

    await peticion('GET', '/api/turnos', { params: { pagina: 0 } })

    expect(ultimaLlamada()[0]).toBe('/api/turnos?pagina=0')
  })
})

describe('respuestas correctas', () => {
  it('devuelve el JSON de la respuesta ya convertido a objeto', async () => {
    fetchFalso.mockResolvedValue(respuestaJson({ idPaciente: 7, nombre: 'Ana' }))

    const paciente = await peticion<{ idPaciente: number; nombre: string }>('GET', '/api/pacientes/7')

    expect(paciente).toEqual({ idPaciente: 7, nombre: 'Ana' })
  })

  // DELETE responde 204 No Content: no hay cuerpo para leer y no tiene que fallar
  it('con un 204 devuelve undefined sin intentar leer el cuerpo', async () => {
    fetchFalso.mockResolvedValue(new Response(null, { status: 204 }))

    await expect(peticion('DELETE', '/api/turnos/3')).resolves.toBeUndefined()
  })
})

describe('errores', () => {
  it('convierte el error del backend en un ApiError con su mensaje y su codigo', async () => {
    fetchFalso.mockResolvedValue(respuestaJson({ status: 409, error: 'Conflict', message: 'Ya hay otro turno en ese horario' }, 409))

    const error = await capturarError(peticion('POST', '/api/turnos', { cuerpo: {} }))

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(409)
    expect(error.message).toBe('Ya hay otro turno en ese horario')
  })

  it('si el error no es JSON usa un mensaje generico en vez de romperse', async () => {
    fetchFalso.mockResolvedValue(new Response('<html>Bad gateway</html>', { status: 502 }))

    const error = await capturarError(peticion('GET', '/api/pacientes'))

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(502)
    expect(error.message).toBe('Ocurrió un error inesperado. Intentá de nuevo más tarde.')
  })

  it('si no hay conexion con el servidor avisa con codigo 0', async () => {
    fetchFalso.mockRejectedValue(new TypeError('Failed to fetch'))

    const error = await capturarError(peticion('GET', '/api/pacientes'))

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(0)
    expect(error.message).toContain('No se pudo conectar con el servidor')
  })
})

describe('sesion vencida (401)', () => {
  it('avisa para volver al login y devuelve un mensaje claro', async () => {
    const alNoAutorizado = vi.fn()
    configurarCliente({ alNoAutorizado })
    fetchFalso.mockResolvedValue(new Response('', { status: 401 }))

    const error = await capturarError(peticion('GET', '/api/turnos'))

    expect(alNoAutorizado).toHaveBeenCalledOnce()
    expect(error.status).toBe(401)
    expect(error.message).toBe('Tu sesión venció. Iniciá sesión de nuevo.')
  })

  // en el login un 401 significa "clave incorrecta", no "se vencio la sesion": no hay que mandar al usuario al login (ya esta ahi)
  it('en las llamadas publicas (login) no dispara el aviso y muestra el mensaje del backend', async () => {
    const alNoAutorizado = vi.fn()
    configurarCliente({ alNoAutorizado })
    fetchFalso.mockResolvedValue(respuestaJson({ status: 401, error: 'Unauthorized', message: 'Usuario o contraseña incorrectos' }, 401))

    const error = await capturarError(peticion('POST', '/api/auth/login', { cuerpo: {}, publica: true }))

    expect(alNoAutorizado).not.toHaveBeenCalled()
    expect(error.message).toBe('Usuario o contraseña incorrectos')
  })

  it('otros errores del cliente (404) no cierran la sesion', async () => {
    const alNoAutorizado = vi.fn()
    configurarCliente({ alNoAutorizado })
    fetchFalso.mockResolvedValue(respuestaJson({ status: 404, error: 'Not Found', message: 'Paciente no encontrado' }, 404))

    await peticion('GET', '/api/pacientes/999').catch(() => {})

    expect(alNoAutorizado).not.toHaveBeenCalled()
  })
})
