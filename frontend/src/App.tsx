import { Route, Routes } from 'react-router-dom'
import Layout from './components/layout/Layout'
import RutaProtegida from './components/ui/RutaProtegida'
import Agenda from './pages/Agenda'
import Login from './pages/Login'
import NoEncontrada from './pages/NoEncontrada'
import PacienteForm from './pages/PacienteForm'
import Pacientes from './pages/Pacientes'
import PerfilPaciente from './pages/PerfilPaciente'
import TurnoForm from './pages/TurnoForm'
import Turnos from './pages/Turnos'

/*
 Mapa de rutas de la aplicación. Reemplaza a los servlets de la versión anterior:
 antes cada URL (/SvTurnos, /SvPacientes...) apuntaba a un servlet; ahora cada ruta apunta a una pantalla de React.

 Las rutas anidadas funcionan así: RutaProtegida deja pasar solo si hay sesión, y Layout dibuja el menú y la barra superior;
 la pantalla de cada ruta aparece dentro del Layout (en su <Outlet />).
 */
export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />

      <Route element={<RutaProtegida />}>
        <Route element={<Layout />}>
          <Route index element={<Agenda />} />

          <Route path="turnos" element={<Turnos />} />
          <Route path="turnos/nuevo" element={<TurnoForm />} />
          <Route path="turnos/:idTurno/editar" element={<TurnoForm />} />

          <Route path="pacientes" element={<Pacientes />} />
          <Route path="pacientes/nuevo" element={<PacienteForm />} />
          <Route path="pacientes/:idPaciente" element={<PerfilPaciente />} />
          <Route path="pacientes/:idPaciente/editar" element={<PacienteForm />} />

          <Route path="*" element={<NoEncontrada />} />
        </Route>
      </Route>
    </Routes>
  )
}
