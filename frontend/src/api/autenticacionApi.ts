import type { UsuarioActual } from '../types'
import { peticion } from './cliente'

/*
 Las tres llamadas son "públicas" para el cliente HTTP: un 401 en ellas es normal (clave incorrecta, o todavía no hay sesión)
 y NO debe interpretarse como "se venció la sesión".
 */
export const autenticacionApi = {
  login: (usuario: string, contrasenia: string) =>
    peticion<UsuarioActual>('POST', '/api/auth/login', { cuerpo: { usuario, contrasenia }, publica: true }),

  // ¿hay una sesión abierta? Se consulta al cargar la página, porque la cookie de sesión sobrevive a recargarla
  usuarioActual: () => peticion<UsuarioActual>('GET', '/api/auth/me', { publica: true }),

  // el servidor destruye la sesión
  logout: () => peticion<void>('POST', '/api/auth/logout', { publica: true }),
}
