import { Link, useLocation } from 'react-router-dom'
import { NOMBRE_CONSULTORIO, NOMBRE_PROFESIONAL } from '../../config/consultorio'

interface Props {
  colapsado: boolean
  alAlternar: () => void
}

interface ItemProps {
  a: string
  icono: string
  texto: string
  activo: boolean
}

// un renglón del menú. SB Admin 2 marca el renglón actual con la clase "active" en el <li>
function ItemMenu({ a, icono, texto, activo }: ItemProps) {
  return (
    <li className={`nav-item ${activo ? 'active' : ''}`}>
      <Link className="nav-link" to={a}>
        <i className={`fas ${icono}`}></i>
        <span>{texto}</span>
      </Link>
    </li>
  )
}

export default function Sidebar({ colapsado, alAlternar }: Props) {
  const { pathname } = useLocation()

  /*
   Qué renglón se resalta según la URL.
   Agenda ("/") se compara exacto, porque TODAS las rutas empiezan con "/".
   "Pacientes" se resalta también en el perfil y la edición, pero NO en /pacientes/nuevo (que tiene su propio renglón).
   */
  const enAgenda = pathname === '/'
  const enTurnos = pathname.startsWith('/turnos')
  const enNuevoPaciente = pathname === '/pacientes/nuevo'
  const enPacientes = pathname.startsWith('/pacientes') && !enNuevoPaciente

  return (
    <ul className={`navbar-nav bg-gradient-primary sidebar sidebar-dark accordion ${colapsado ? 'toggled' : ''}`} id="accordionSidebar">
      <Link className="sidebar-brand d-flex align-items-center justify-content-center" to="/">
        <div className="sidebar-brand-icon">
          <i className="fas fa-tooth"></i>
        </div>
        <div className="sidebar-brand-text mx-3">
          {NOMBRE_CONSULTORIO.split(' ')[0]}
          <br />
          <small>{NOMBRE_PROFESIONAL}</small>
        </div>
      </Link>

      <hr className="sidebar-divider my-0" />

      <ItemMenu a="/" icono="fa-calendar-alt" texto="Agenda" activo={enAgenda} />
      <ItemMenu a="/turnos" icono="fa-clipboard-list" texto="Turnos" activo={enTurnos} />

      <hr className="sidebar-divider" />

      <div className="sidebar-heading">Gestión</div>

      <ItemMenu a="/pacientes" icono="fa-users" texto="Pacientes" activo={enPacientes} />
      <ItemMenu a="/pacientes/nuevo" icono="fa-user-plus" texto="Nuevo paciente" activo={enNuevoPaciente} />

      <hr className="sidebar-divider d-none d-md-block" />

      {/* botón redondo para achicar el menú a solo íconos (solo en pantallas medianas y grandes) */}
      <div className="text-center d-none d-md-inline">
        <button className="rounded-circle border-0" id="sidebarToggle" onClick={alAlternar} aria-label="Alternar menú lateral"></button>
      </div>
    </ul>
  )
}
