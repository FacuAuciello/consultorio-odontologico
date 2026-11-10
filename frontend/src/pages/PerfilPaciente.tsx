import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ApiError } from '../api/cliente'
import { pacientesApi } from '../api/pacientesApi'
import Avatar from '../components/ui/Avatar'
import Cargando from '../components/ui/Cargando'
import EncabezadoPagina from '../components/ui/EncabezadoPagina'
import ErrorCarga from '../components/ui/ErrorCarga'
import EstadoVacio from '../components/ui/EstadoVacio'
import { useAvisos } from '../context/AvisosContext'
import { useConsulta } from '../hooks/useConsulta'
import { calcularEstado } from '../utils/estadoTurno'
import { calcularEdad, formatearDiaCorto, formatearFecha, formatearHora, parsearFechaHora } from '../utils/fechas'
import { apellidoYNombre, formatearMonto } from '../utils/formato'

/*
 Perfil de un paciente: datos personales, historia clínica editable y listado de sus turnos.
 */
export default function PerfilPaciente() {
  const { idPaciente } = useParams()
  const id = Number(idPaciente)
  const { mostrarAviso } = useAvisos()

  // las dos consultas son independientes: cada una tiene su propio estado de carga / error
  const paciente = useConsulta(() => pacientesApi.obtener(id), [id])
  const turnos = useConsulta(() => pacientesApi.turnosDelPaciente(id), [id])

  const [historia, setHistoria] = useState('')
  const [guardando, setGuardando] = useState(false)

  // al llegar el paciente se copia su historia al textarea (que es un campo editable, con su propio estado)
  useEffect(() => {
    if (paciente.datos) setHistoria(paciente.datos.historiaClinica ?? '')
  }, [paciente.datos])

  const guardarHistoria = async () => {
    setGuardando(true)
    try {
      // PATCH: solo viaja la historia clínica, no todo el paciente
      await pacientesApi.actualizarHistoriaClinica(id, historia)
      mostrarAviso('exito', 'Historia clínica guardada correctamente.')
      paciente.recargar()
    } catch (e) {
      mostrarAviso('error', e instanceof ApiError ? e.message : 'No se pudo guardar la historia clínica.')
    } finally {
      setGuardando(false)
    }
  }

  if (paciente.cargando && !paciente.datos) return <Cargando texto="Cargando paciente..." />
  if (paciente.error) return <ErrorCarga mensaje={paciente.error} alReintentar={paciente.recargar} />
  if (!paciente.datos) return null

  const p = paciente.datos
  const hayCambios = historia !== (p.historiaClinica ?? '')
  const ahora = new Date()

  return (
    <>
      <EncabezadoPagina titulo={apellidoYNombre(p)}>
        <Link to={`/turnos/nuevo?idPaciente=${p.idPaciente}`} className="btn btn-primary btn-sm shadow-sm">
          <i className="fas fa-calendar-plus fa-sm text-white-50"></i> Nuevo turno
        </Link>
        <Link to={`/pacientes/${p.idPaciente}/editar`} className="btn btn-warning btn-sm shadow-sm">
          <i className="fas fa-edit fa-sm"></i> Editar datos
        </Link>
      </EncabezadoPagina>

      <div className="row">
        {/* ---------- columna izquierda: datos personales ---------- */}
        <div className="col-lg-4">
          <div className="card shadow mb-4">
            <div className="card-header py-3">
              <h6 className="m-0 font-weight-bold text-primary">Datos personales</h6>
            </div>
            <div className="card-body">
              <div className="text-center mb-4">
                <Avatar nombre={p.nombre} apellido={p.apellido} grande />
                <h5 className="mt-3 mb-0 font-weight-bold text-gray-800">
                  {p.nombre} {p.apellido}
                </h5>
                <span className="text-gray-600">{calcularEdad(p.fechaNacimiento)} años</span>
              </div>
              <dl className="datos-paciente mb-0">
                <dt>DNI</dt>
                <dd>{p.dni}</dd>
                <dt>Fecha de nacimiento</dt>
                <dd>{formatearFecha(p.fechaNacimiento)}</dd>
                <dt>Teléfono</dt>
                <dd>{p.telefono}</dd>
                <dt>Dirección</dt>
                <dd>{p.direccion}</dd>
              </dl>
            </div>
          </div>
        </div>

        {/* ---------- columna derecha: historia clínica + turnos ---------- */}
        <div className="col-lg-8">
          <div className="card shadow mb-4">
            <div className="card-header py-3 d-flex justify-content-between align-items-center">
              <h6 className="m-0 font-weight-bold text-primary">Historia clínica</h6>
              <button className="btn btn-primary btn-sm" onClick={guardarHistoria} disabled={guardando || !hayCambios}>
                <i className="fas fa-save mr-1"></i> {guardando ? 'Guardando...' : 'Guardar'}
              </button>
            </div>
            <div className="card-body">
              <textarea
                className="form-control historia-clinica"
                rows={10}
                aria-label="Historia clínica"
                placeholder="Sin anotaciones todavía. Las notas y montos de cada turno se agregan acá automáticamente."
                value={historia}
                onChange={(e) => setHistoria(e.target.value)}
              />
              <small className="form-text text-muted">Acá podés agregar antecedentes, alergias o cualquier dato que quieras tener a mano.</small>
            </div>
          </div>

          <div className="card shadow mb-4">
            <div className="card-header py-3">
              <h6 className="m-0 font-weight-bold text-primary">Turnos del paciente</h6>
            </div>
            <div className="card-body">
              {turnos.cargando && !turnos.datos && <Cargando />}
              {turnos.error && <ErrorCarga mensaje={turnos.error} alReintentar={turnos.recargar} />}
              {turnos.datos && turnos.datos.length === 0 && (
                <EstadoVacio icono="fa-calendar-times" titulo="Este paciente todavía no tiene turnos">
                  <Link to={`/turnos/nuevo?idPaciente=${p.idPaciente}`}>Agendar el primero</Link>
                </EstadoVacio>
              )}
              {turnos.datos && turnos.datos.length > 0 && (
                <div className="table-responsive">
                  <table className="table tabla-turnos align-middle mb-0">
                    <thead>
                      <tr>
                        <th>Fecha y hora</th>
                        <th>Duración</th>
                        <th className="text-right">Monto</th>
                        <th>Estado</th>
                        <th className="text-right">Editar</th>
                      </tr>
                    </thead>
                    <tbody>
                      {turnos.datos.map((turno) => {
                        const inicio = parsearFechaHora(turno.fechaHora)
                        const estado = calcularEstado(turno, ahora)
                        return (
                          <tr key={turno.idTurno}>
                            <td>
                              <span className="font-weight-bold text-capitalize">{formatearDiaCorto(inicio)}</span>{' '}
                              <span className="text-gray-600">{formatearHora(inicio)}</span>
                            </td>
                            <td>{turno.duracionMinutos} min</td>
                            <td className="text-right text-nowrap">{turno.monto !== null ? formatearMonto(turno.monto) : '–'}</td>
                            <td>
                              <span className={`badge ${estado.claseBadge}`}>{estado.texto}</span>
                            </td>
                            <td className="text-right">
                              <Link to={`/turnos/${turno.idTurno}/editar`} className="btn btn-warning btn-circle btn-sm" title="Editar turno" aria-label="Editar turno">
                                <i className="fas fa-edit"></i>
                              </Link>
                            </td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </>
  )
}
