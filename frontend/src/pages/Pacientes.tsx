import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ApiError } from '../api/cliente'
import { pacientesApi } from '../api/pacientesApi'
import Avatar from '../components/ui/Avatar'
import Cargando from '../components/ui/Cargando'
import ConfirmDialog from '../components/ui/ConfirmDialog'
import EncabezadoPagina from '../components/ui/EncabezadoPagina'
import ErrorCarga from '../components/ui/ErrorCarga'
import EstadoVacio from '../components/ui/EstadoVacio'
import Paginacion from '../components/ui/Paginacion'
import { useAvisos } from '../context/AvisosContext'
import { useConsulta } from '../hooks/useConsulta'
import { useDebounce } from '../hooks/useDebounce'
import type { PacienteResumen } from '../types'
import { calcularEdad } from '../utils/fechas'
import { apellidoYNombre } from '../utils/formato'

const TAMANIO_PAGINA = 10

export default function Pacientes() {
  const { mostrarAviso } = useAvisos()

  const [busqueda, setBusqueda] = useState('')
  const [pagina, setPagina] = useState(0)
  const [aEliminar, setAEliminar] = useState<PacienteResumen | null>(null)
  const [eliminando, setEliminando] = useState(false)

  /*
   La búsqueda se hace en el servidor (GET /api/pacientes?busqueda=...). Con "debounce" la consulta sale recién cuando el usuario
   deja de tipear 300 ms: sin eso se haría una petición por cada letra.
   */
  const busquedaDemorada = useDebounce(busqueda.trim(), 300)
  const { datos, cargando, error, recargar } = useConsulta(() => pacientesApi.listar(busquedaDemorada), [busquedaDemorada])

  const pacientes = datos ?? []
  const paginaVisible = Math.min(pagina, Math.max(0, Math.ceil(pacientes.length / TAMANIO_PAGINA) - 1))
  const filas = pacientes.slice(paginaVisible * TAMANIO_PAGINA, (paginaVisible + 1) * TAMANIO_PAGINA)

  const confirmarEliminacion = async () => {
    if (!aEliminar) return
    setEliminando(true)
    try {
      await pacientesApi.eliminar(aEliminar.idPaciente)
      mostrarAviso(
        'exito',
        <>
          Paciente <strong>{apellidoYNombre(aEliminar)}</strong> eliminado correctamente.
        </>,
      )
      recargar()
    } catch (e) {
      mostrarAviso('error', e instanceof ApiError ? e.message : 'Ocurrió un error al eliminar el paciente.')
    } finally {
      setEliminando(false)
      setAEliminar(null)
    }
  }

  return (
    <>
      <EncabezadoPagina titulo="Pacientes">
        <Link to="/pacientes/nuevo" className="btn btn-primary btn-sm shadow-sm">
          <i className="fas fa-user-plus fa-sm text-white-50"></i> Nuevo paciente
        </Link>
      </EncabezadoPagina>

      <div className="card shadow mb-4">
        <div className="card-header py-3">
          <div className="input-group input-group-sm buscador-pacientes">
            <div className="input-group-prepend">
              <span className="input-group-text bg-light border-0">
                <i className="fas fa-search"></i>
              </span>
            </div>
            <input
              className="form-control bg-light border-0 small"
              type="search"
              placeholder="Buscar por nombre, apellido o DNI..."
              aria-label="Buscar pacientes"
              value={busqueda}
              onChange={(e) => {
                setBusqueda(e.target.value)
                setPagina(0)
              }}
            />
          </div>
        </div>

        <div className="card-body">
          {cargando && !datos && <Cargando texto="Cargando pacientes..." />}
          {error && <ErrorCarga mensaje={error} alReintentar={recargar} />}

          {datos && pacientes.length === 0 && (
            <EstadoVacio icono="fa-users" titulo={busquedaDemorada ? 'Ningún paciente coincide con la búsqueda' : 'Todavía no hay pacientes'}>
              {busquedaDemorada ? 'Revisá el nombre o el DNI.' : 'Registrá el primero con el botón "Nuevo paciente".'}
            </EstadoVacio>
          )}

          {datos && pacientes.length > 0 && (
            <>
              <p className="small text-gray-700 mb-3">
                <strong>{pacientes.length}</strong> {pacientes.length === 1 ? 'paciente' : 'pacientes'}
              </p>
              <div className="table-responsive">
                <table className="table tabla-turnos tabla-pacientes align-middle mb-0">
                  <thead>
                    <tr>
                      <th>Paciente</th>
                      <th>DNI</th>
                      <th className="d-none d-md-table-cell">Teléfono</th>
                      <th className="d-none d-lg-table-cell">Dirección</th>
                      <th className="d-none d-md-table-cell">Edad</th>
                      <th className="text-right">Acciones</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filas.map((paciente) => (
                      <tr key={paciente.idPaciente}>
                        <td className="celda-paciente">
                          <Link to={`/pacientes/${paciente.idPaciente}`} className="d-flex align-items-center text-gray-800 enlace-paciente">
                            <Avatar nombre={paciente.nombre} apellido={paciente.apellido} />
                            <span className="ml-2 font-weight-bold">{apellidoYNombre(paciente)}</span>
                          </Link>
                        </td>
                        <td className="celda-dni">{paciente.dni}</td>
                        <td className="d-none d-md-table-cell">{paciente.telefono}</td>
                        <td className="d-none d-lg-table-cell">{paciente.direccion}</td>
                        <td className="d-none d-md-table-cell">{calcularEdad(paciente.fechaNacimiento)} años</td>
                        <td className="text-right text-nowrap celda-acciones">
                          <Link to={`/pacientes/${paciente.idPaciente}`} className="btn btn-info btn-circle btn-sm" title="Ver historia clínica" aria-label="Ver historia clínica">
                            <i className="fas fa-notes-medical"></i>
                          </Link>{' '}
                          <Link to={`/pacientes/${paciente.idPaciente}/editar`} className="btn btn-warning btn-circle btn-sm" title="Editar paciente" aria-label="Editar paciente">
                            <i className="fas fa-edit"></i>
                          </Link>{' '}
                          <button className="btn btn-danger btn-circle btn-sm" title="Eliminar paciente" aria-label="Eliminar paciente" onClick={() => setAEliminar(paciente)}>
                            <i className="fas fa-trash"></i>
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <Paginacion pagina={paginaVisible} tamanio={TAMANIO_PAGINA} total={pacientes.length} alCambiar={setPagina} />
            </>
          )}
        </div>
      </div>

      {aEliminar && (
        <ConfirmDialog titulo="Eliminar paciente" textoConfirmar="Eliminar" procesando={eliminando} alConfirmar={confirmarEliminacion} alCancelar={() => setAEliminar(null)}>
          ¿Eliminar a <strong>{apellidoYNombre(aEliminar)}</strong>? También se borrarán todos sus turnos y su historia clínica. Esta acción no se puede deshacer.
        </ConfirmDialog>
      )}
    </>
  )
}
