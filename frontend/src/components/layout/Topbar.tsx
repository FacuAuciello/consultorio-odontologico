import { useAuth } from '../../context/AuthContext'

interface Props {
  alAlternarMenu: () => void
}

export default function Topbar({ alAlternarMenu }: Props) {
  const { nombreUsuario, cerrarSesion } = useAuth()

  return (
    <nav className="navbar navbar-expand navbar-light bg-white topbar mb-4 static-top shadow">
      {/* botón hamburguesa: solo se ve en pantallas chicas (d-md-none) */}
      <button id="sidebarToggleTop" className="btn btn-link d-md-none rounded-circle mr-3" onClick={alAlternarMenu} aria-label="Abrir menú">
        <i className="fa fa-bars"></i>
      </button>

      <ul className="navbar-nav ml-auto align-items-center">
        <li className="nav-item d-none d-sm-block mr-3 text-gray-600 small">
          <i className="fas fa-user-circle fa-fw mr-1 text-gray-400"></i>
          {nombreUsuario}
        </li>
        <li className="nav-item">
          <button className="btn btn-link nav-link" onClick={cerrarSesion}>
            <i className="fas fa-sign-out-alt fa-sm fa-fw mr-2 text-gray-400"></i>
            Cerrar sesión
          </button>
        </li>
      </ul>
    </nav>
  )
}
