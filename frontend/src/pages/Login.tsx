import { useEffect, useState, type FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/cliente'
import { NOMBRE_CONSULTORIO, NOMBRE_PROFESIONAL } from '../config/consultorio'
import { useAuth } from '../context/AuthContext'

export default function Login() {
  const { autenticado, verificando, iniciarSesion } = useAuth()
  const navigate = useNavigate()
  const ubicacion = useLocation()

  const [usuario, setUsuario] = useState('')
  const [contrasenia, setContrasenia] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  // SB Admin 2 pinta el fondo degradado azul con una clase en el <body>; se agrega solo mientras esta pantalla está abierta
  useEffect(() => {
    document.body.classList.add('bg-gradient-primary')
    return () => document.body.classList.remove('bg-gradient-primary')
  }, [])

  // la ruta a la que se quería entrar antes de que pidiera login (ver RutaProtegida)
  const destino = (ubicacion.state as { from?: string } | null)?.from ?? '/'

  // mientras se verifica si ya hay una sesión abierta no se muestra el formulario (evita un parpadeo)
  if (verificando) return null

  // si ya hay sesión, el login no tiene sentido: se va directo a la agenda
  if (autenticado) return <Navigate to={destino} replace />

  const manejarEnvio = async (evento: FormEvent) => {
    evento.preventDefault() // sin esto el navegador recargaría la página al enviar el formulario
    setError(null)
    setEnviando(true)

    try {
      await iniciarSesion(usuario.trim(), contrasenia)
      navigate(destino, { replace: true })
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Ocurrió un error inesperado')
      setEnviando(false)
    }
  }

  return (
    <div className="container">
      <div className="row justify-content-center">
        <div className="col-xl-5 col-lg-6 col-md-8">
          <div className="card o-hidden border-0 shadow-lg my-5">
            <div className="card-body p-0">
              <div className="p-5">
                <div className="text-center mb-4">
                  <i className="fas fa-tooth fa-3x text-primary mb-3"></i>
                  <h1 className="h4 text-gray-900 mb-1">{NOMBRE_CONSULTORIO}</h1>
                  <p className="text-muted small">{NOMBRE_PROFESIONAL}</p>
                </div>

                {error && (
                  <div className="alert alert-danger text-center small" role="alert">
                    <i className="fas fa-exclamation-circle mr-1"></i> {error}
                  </div>
                )}

                <form onSubmit={manejarEnvio}>
                  <div className="form-group">
                    <label htmlFor="usuario" className="small font-weight-bold text-uppercase text-muted">
                      Usuario
                    </label>
                    <input
                      id="usuario"
                      className="form-control form-control-user"
                      placeholder="Nombre de usuario"
                      autoComplete="username"
                      autoFocus
                      required
                      value={usuario}
                      onChange={(e) => setUsuario(e.target.value)}
                    />
                  </div>
                  <div className="form-group">
                    <label htmlFor="contrasenia" className="small font-weight-bold text-uppercase text-muted">
                      Contraseña
                    </label>
                    <input
                      id="contrasenia"
                      type="password"
                      className="form-control form-control-user"
                      placeholder="••••••••"
                      autoComplete="current-password"
                      required
                      value={contrasenia}
                      onChange={(e) => setContrasenia(e.target.value)}
                    />
                  </div>
                  <button type="submit" className="btn btn-primary btn-user btn-block" disabled={enviando}>
                    {enviando && <span className="spinner-border spinner-border-sm mr-2" aria-hidden="true"></span>}
                    Ingresar
                  </button>
                </form>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
