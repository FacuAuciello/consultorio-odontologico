import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import Cargando from './Cargando'

/*
 Guardia de rutas: si no hay sesión, redirige al login. Se guarda la ruta que se intentaba abrir ("from")
 para volver a ella después de iniciar sesión.
 OJO: esto es solo comodidad de pantalla. La seguridad REAL está en el backend, que rechaza con 401 cualquier pedido sin una sesión válida.
 */
export default function RutaProtegida() {
  const { autenticado, verificando } = useAuth()
  const ubicacion = useLocation()

  // al recargar la página se le pregunta al servidor si la sesión sigue abierta: hasta tener la respuesta no se sabe si hay que ir al login
  if (verificando) return <Cargando />

  if (!autenticado) {
    return <Navigate to="/login" replace state={{ from: ubicacion.pathname + ubicacion.search }} />
  }

  return <Outlet />
}
