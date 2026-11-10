import { API_URL } from '../config/consultorio'
import type { RespuestaError } from '../types'

/*
 Cliente HTTP único: todas las llamadas a la API pasan por la función peticion().
 Se encarga de lo que sería repetitivo en cada pantalla:
   - enviar la cookie de sesión (credentials: 'include'): así el servidor sabe quién está logueado
   - convertir el cuerpo a JSON y la respuesta de JSON a objeto
   - transformar los errores del backend ({status, error, message}) en una excepción ApiError con un mensaje legible
   - si el servidor responde 401 (la sesión venció) avisar para volver a la pantalla de login
 */

// error que lanzan todas las llamadas fallidas; "mensaje" se muestra tal cual al usuario
export class ApiError extends Error {
  readonly status: number

  constructor(status: number, mensaje: string) {
    super(mensaje)
    this.name = 'ApiError'
    this.status = status
  }
}

/*
 El cliente no importa el contexto de autenticación (sería una dependencia circular: el contexto usa la API para el login).
 En cambio, AuthContext le "enchufa" esta función al arrancar: qué hacer cuando el servidor responde 401.
 */
interface ConfiguracionCliente {
  alNoAutorizado: () => void
}

let configuracion: ConfiguracionCliente = { alNoAutorizado: () => {} }

export function configurarCliente(nueva: ConfiguracionCliente): void {
  configuracion = nueva
}

type Parametros = Record<string, string | number | null | undefined>

interface Opciones {
  cuerpo?: unknown
  params?: Parametros
  // las llamadas de autenticación (login, me, logout) no disparan el "se venció la sesión" ante un 401
  publica?: boolean
}

function armarUrl(ruta: string, params?: Parametros): string {
  const url = new URL(`${API_URL}${ruta}`, window.location.origin)

  // los parámetros vacíos (null/undefined/"") no se envían: así los filtros opcionales quedan fuera de la URL
  Object.entries(params ?? {}).forEach(([clave, valor]) => {
    if (valor !== null && valor !== undefined && valor !== '') url.searchParams.set(clave, String(valor))
  })

  // si la API es del mismo origen se devuelve solo la ruta (más legible en la pestaña Network)
  return API_URL === '' ? `${url.pathname}${url.search}` : url.toString()
}

export async function peticion<T>(metodo: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE', ruta: string, opciones: Opciones = {}): Promise<T> {
  const headers: Record<string, string> = {}

  if (opciones.cuerpo !== undefined) headers['Content-Type'] = 'application/json'

  let respuesta: Response
  try {
    respuesta = await fetch(armarUrl(ruta, opciones.params), {
      method: metodo,
      headers,
      // "include": el navegador envía y guarda la cookie de sesión aunque la API esté en otro origen (en desarrollo el proxy de Vite la hace del mismo origen)
      credentials: 'include',
      body: opciones.cuerpo !== undefined ? JSON.stringify(opciones.cuerpo) : undefined,
    })
  } catch {
    // fetch solo lanza excepción cuando no hay conexión con el servidor (apagado, sin internet, CORS bloqueado)
    throw new ApiError(0, 'No se pudo conectar con el servidor. Revisá tu conexión e intentá de nuevo.')
  }

  if (respuesta.ok) {
    // 204 No Content (DELETE): no hay cuerpo para leer
    return (respuesta.status === 204 ? undefined : await respuesta.json()) as T
  }

  if (respuesta.status === 401 && !opciones.publica) {
    configuracion.alNoAutorizado()
  }

  throw new ApiError(respuesta.status, await extraerMensaje(respuesta))
}

// el backend responde siempre {status, error, message}; si por algún motivo no es JSON se usa un mensaje genérico
async function extraerMensaje(respuesta: Response): Promise<string> {
  try {
    const error = (await respuesta.json()) as Partial<RespuestaError>
    if (error.message) return error.message
  } catch {
    // el cuerpo no era JSON: se cae al mensaje genérico de abajo
  }
  return respuesta.status === 401
    ? 'Tu sesión venció. Iniciá sesión de nuevo.'
    : 'Ocurrió un error inesperado. Intentá de nuevo más tarde.'
}
