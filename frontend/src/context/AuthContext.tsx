import { createContext, useCallback, useContext, useEffect, useLayoutEffect, useMemo, useState, type ReactNode } from 'react'
import { autenticacionApi } from '../api/autenticacionApi'
import { configurarCliente } from '../api/cliente'

/*
 Contexto de autenticación: guarda quién está logueado y expone iniciarSesion() / cerrarSesion() a toda la app.
 Un contexto de React es una "variable global" controlada: cualquier componente puede leerla con useAuth()
 sin tener que pasarla de padre a hijo por props.

 La sesión vive en el SERVIDOR: el navegador solo guarda una cookie HttpOnly que JavaScript ni siquiera puede leer.
 Por eso acá no se guarda ningún token: solo el nombre del usuario, para mostrarlo en pantalla.
 */

interface AuthContextoValor {
  nombreUsuario: string | null
  autenticado: boolean
  // true mientras se le pregunta al servidor si hay una sesión abierta (al cargar o recargar la página)
  verificando: boolean
  iniciarSesion: (usuario: string, contrasenia: string) => Promise<void>
  cerrarSesion: () => Promise<void>
}

const AuthContext = createContext<AuthContextoValor | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [nombreUsuario, setNombreUsuario] = useState<string | null>(null)
  const [verificando, setVerificando] = useState(true)

  /*
   Al cargar la página se pregunta "¿quién soy?" (GET /api/auth/me). Si la cookie de sesión sigue siendo válida,
   el servidor responde con el usuario y no hace falta volver a loguearse; si no, responde 401 y se queda en null.
   El flag "cancelado" evita actualizar el estado si el componente se desmontó antes de que llegue la respuesta.
   */
  useEffect(() => {
    let cancelado = false

    autenticacionApi
      .usuarioActual()
      .then((usuario) => !cancelado && setNombreUsuario(usuario.nombreUsuario))
      .catch(() => {
        // 401 = no hay sesión: es el caso normal de quien todavía no inició sesión
      })
      .finally(() => !cancelado && setVerificando(false))

    return () => {
      cancelado = true
    }
  }, [])

  const iniciarSesion = useCallback(async (usuario: string, contrasenia: string) => {
    const respuesta = await autenticacionApi.login(usuario, contrasenia)
    setNombreUsuario(respuesta.nombreUsuario)
  }, [])

  const cerrarSesion = useCallback(async () => {
    try {
      await autenticacionApi.logout()
    } catch {
      // si el servidor no responde igual se cierra la sesión en pantalla: la cookie vence sola
    }
    setNombreUsuario(null)
  }, [])

  /*
   Le "enchufa" al cliente HTTP qué hacer si el servidor responde 401 en medio del trabajo (la sesión venció): volver al login.
   Tiene que ser useLayoutEffect y no useEffect: React ejecuta los efectos de los hijos ANTES que los del padre, así que con useEffect
   una pantalla que consulta la API apenas se monta podría recibir un 401 antes de que el cliente esté configurado.
   Los efectos "layout" corren antes que cualquier useEffect de la app.
   */
  useLayoutEffect(() => {
    configurarCliente({ alNoAutorizado: () => setNombreUsuario(null) })
  }, [])

  // useMemo evita crear un objeto nuevo en cada render: los componentes que leen el contexto solo se actualizan si algo cambió de verdad
  const valor = useMemo<AuthContextoValor>(
    () => ({
      nombreUsuario,
      autenticado: nombreUsuario !== null,
      verificando,
      iniciarSesion,
      cerrarSesion,
    }),
    [nombreUsuario, verificando, iniciarSesion, cerrarSesion],
  )

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>
}

// hook de acceso: falla con un mensaje claro si se usa fuera del AuthProvider
export function useAuth(): AuthContextoValor {
  const contexto = useContext(AuthContext)
  if (!contexto) throw new Error('useAuth tiene que usarse dentro de <AuthProvider>')
  return contexto
}
