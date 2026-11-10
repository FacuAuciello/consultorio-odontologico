import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { agendaApi } from '../api/agendaApi'
import { ApiError } from '../api/cliente'
import { pacientesApi } from '../api/pacientesApi'
import { turnosApi } from '../api/turnosApi'
import Campo from '../components/forms/Campo'
import MontoInput from '../components/forms/MontoInput'
import PacienteSelect from '../components/forms/PacienteSelect'
import Cargando from '../components/ui/Cargando'
import ConfirmDialog from '../components/ui/ConfirmDialog'
import EncabezadoPagina from '../components/ui/EncabezadoPagina'
import ErrorCarga from '../components/ui/ErrorCarga'
import { useAvisos } from '../context/AvisosContext'
import { useConsulta } from '../hooks/useConsulta'
import { aFechaIso, esDiaLaboral, formatearFecha, formatearHora, generarHorarios, parsearFecha, parsearFechaHora } from '../utils/fechas'
import { apellidoYNombre, nombreDelPaciente } from '../utils/formato'
import { armarLinkWhatsApp } from '../utils/whatsapp'

/*
 Formulario para crear y para editar un turno (misma pantalla, distinta ruta):
   /turnos/nuevo               -> crear. Acepta ?fechaHora=...&idPaciente=... para llegar con datos precargados
                                  (al hacer clic en un hueco del calendario, o desde el perfil de un paciente)
   /turnos/:idTurno/editar     -> editar uno existente
 */
export default function TurnoForm() {
  const { idTurno } = useParams()
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const { mostrarAviso } = useAvisos()

  const editando = idTurno !== undefined

  // lo que necesita el formulario para existir: horario del consultorio, lista de pacientes y (si se edita) el turno
  const { datos, cargando, error, recargar } = useConsulta(
    () => Promise.all([agendaApi.configuracion(), pacientesApi.listar(), editando ? turnosApi.obtener(Number(idTurno)) : Promise.resolve(null)] as const),
    [idTurno],
  )

  // estado del formulario. La fecha y la hora van separadas porque se eligen con controles distintos
  const [fecha, setFecha] = useState('')
  const [hora, setHora] = useState('')
  const [duracion, setDuracion] = useState<number | null>(null)
  const [idPaciente, setIdPaciente] = useState<number | null>(null)
  const [monto, setMonto] = useState<number | null>(null)
  const [notas, setNotas] = useState('')

  const [errorServidor, setErrorServidor] = useState<string | null>(null)
  const [intentoEnviar, setIntentoEnviar] = useState(false)
  const [guardando, setGuardando] = useState(false)
  const [confirmandoBorrado, setConfirmandoBorrado] = useState(false)

  const [config, pacientes, turnoExistente] = datos ?? [null, [], null]

  /*
   Valores iniciales: se cargan UNA vez, cuando llegan los datos de la API.
   - editando: sale del turno guardado
   - creando: sale de la URL (si vino de un clic en el calendario) o queda vacío, con la primera duración permitida
   */
  useEffect(() => {
    if (!datos) return
    const [configuracion, , turno] = datos

    if (turno) {
      const inicio = parsearFechaHora(turno.fechaHora)
      setFecha(aFechaIso(inicio))
      setHora(formatearHora(inicio))
      setDuracion(turno.duracionMinutos)
      setIdPaciente(turno.idPaciente)
      setMonto(turno.monto)
      setNotas(turno.notasOdontologicas ?? '')
      return
    }

    const fechaHoraUrl = params.get('fechaHora') // "2026-10-27T09:30:00"
    const pacienteUrl = Number(params.get('idPaciente'))
    if (fechaHoraUrl) {
      setFecha(fechaHoraUrl.slice(0, 10))
      setHora(fechaHoraUrl.slice(11, 16))
    }
    if (pacienteUrl) setIdPaciente(pacienteUrl)
    setDuracion(configuracion.duracionesPermitidas[0] ?? null)
    // se ejecuta solo cuando llegan los datos: los params de la URL no cambian mientras se está en el formulario
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [datos])

  // horarios disponibles según la duración elegida (un turno de 1 h no puede empezar a las 12:00 si se cierra a las 12:30)
  const horarios = useMemo(() => (config && duracion ? generarHorarios(config, duracion) : []), [config, duracion])

  // si al cambiar la duración la hora elegida ya no entra, se vacía para que el usuario elija de nuevo
  useEffect(() => {
    if (hora && horarios.length > 0 && !horarios.includes(hora)) setHora('')
  }, [horarios, hora])

  // validaciones de pantalla (el backend vuelve a validar todo: la agenda, la superposición, etc.)
  const errores = useMemo(() => {
    const e: { fecha?: string; hora?: string; paciente?: string } = {}
    if (!fecha) e.fecha = 'Elegí una fecha'
    else if (config && !esDiaLaboral(parsearFecha(fecha), config)) e.fecha = 'El consultorio no atiende ese día de la semana'
    if (!hora) e.hora = 'Elegí un horario'
    if (idPaciente === null) e.paciente = 'Elegí un paciente'
    return e
  }, [fecha, hora, idPaciente, config])

  const hayErrores = Object.keys(errores).length > 0

  const guardar = async (evento: FormEvent) => {
    evento.preventDefault()
    setIntentoEnviar(true)
    setErrorServidor(null)
    if (hayErrores || duracion === null || idPaciente === null) return

    setGuardando(true)
    try {
      const cuerpo = {
        fechaHora: `${fecha}T${hora}:00`,
        duracionMinutos: duracion,
        idPaciente,
        monto,
        notasOdontologicas: notas.trim() || null,
      }
      if (editando) await turnosApi.editar(Number(idTurno), cuerpo)
      else await turnosApi.crear(cuerpo)

      mostrarAviso('exito', editando ? 'Turno actualizado correctamente.' : 'Turno agendado correctamente.')
      navigate('/')
    } catch (e) {
      // los errores del servidor (turno superpuesto, fuera de horario...) llegan con un mensaje listo para mostrar
      setErrorServidor(e instanceof ApiError ? e.message : 'Ocurrió un error inesperado al guardar el turno.')
    } finally {
      setGuardando(false)
    }
  }

  const eliminar = async () => {
    setGuardando(true)
    try {
      await turnosApi.eliminar(Number(idTurno))
      mostrarAviso('exito', 'Turno eliminado correctamente.')
      navigate('/')
    } catch (e) {
      setConfirmandoBorrado(false)
      setErrorServidor(e instanceof ApiError ? e.message : 'Ocurrió un error al eliminar el turno.')
    } finally {
      setGuardando(false)
    }
  }

  if (cargando && !datos) return <Cargando texto="Cargando formulario..." />
  if (error) return <ErrorCarga mensaje={error} alReintentar={recargar} />
  if (!config) return null

  const mostrar = (campo: keyof typeof errores) => (intentoEnviar ? errores[campo] : undefined)

  return (
    <>
      <EncabezadoPagina titulo={editando ? 'Editar turno' : 'Nuevo turno'}>
        {/* el recordatorio por WhatsApp solo se ofrece al editar, cuando el turno ya existe */}
        {turnoExistente && (
          <a href={armarLinkWhatsApp(turnoExistente)} target="_blank" rel="noreferrer" className="btn btn-success btn-sm shadow-sm">
            <i className="fab fa-whatsapp fa-sm"></i> Recordar por WhatsApp
          </a>
        )}
      </EncabezadoPagina>

      <div className="row">
        <div className="col-xl-8 col-lg-10">
          <div className="card shadow mb-4">
            <div className="card-header py-3">
              <h6 className="m-0 font-weight-bold text-primary">
                {turnoExistente ? `Turno de ${nombreDelPaciente(turnoExistente)}` : 'Datos del turno'}
              </h6>
            </div>
            <div className="card-body">
              {errorServidor && (
                <div className="alert alert-danger" role="alert">
                  <i className="fas fa-exclamation-triangle mr-2"></i>
                  {errorServidor}
                </div>
              )}

              <form onSubmit={guardar} noValidate>
                <Campo etiqueta="Paciente" htmlFor="paciente" obligatorio error={mostrar('paciente')}>
                  <PacienteSelect id="paciente" pacientes={pacientes} valor={idPaciente} alCambiar={setIdPaciente} hayError={Boolean(mostrar('paciente'))} />
                  {pacientes.length === 0 && (
                    <small className="form-text text-muted">
                      Todavía no hay pacientes. <Link to="/pacientes/nuevo">Registrá el primero</Link>.
                    </small>
                  )}
                </Campo>

                <div className="form-row">
                  <div className="col-md-4">
                    <Campo etiqueta="Duración" htmlFor="duracion" obligatorio>
                      <select id="duracion" className="form-control" value={duracion ?? ''} onChange={(e) => setDuracion(Number(e.target.value))}>
                        {config.duracionesPermitidas.map((minutos) => (
                          <option key={minutos} value={minutos}>
                            {minutos === 60 ? '1 hora' : `${minutos} minutos`}
                          </option>
                        ))}
                      </select>
                    </Campo>
                  </div>
                  <div className="col-md-4">
                    <Campo etiqueta="Fecha" htmlFor="fecha" obligatorio error={mostrar('fecha')}>
                      <input
                        id="fecha"
                        type="date"
                        className={`form-control ${mostrar('fecha') ? 'is-invalid' : ''}`}
                        value={fecha}
                        onChange={(e) => setFecha(e.target.value)}
                      />
                    </Campo>
                  </div>
                  <div className="col-md-4">
                    <Campo etiqueta="Horario" htmlFor="hora" obligatorio error={mostrar('hora')}>
                      <select id="hora" className={`form-control ${mostrar('hora') ? 'is-invalid' : ''}`} value={hora} onChange={(e) => setHora(e.target.value)}>
                        <option value="">Elegir...</option>
                        {horarios.map((h) => (
                          <option key={h} value={h}>
                            {h} hs
                          </option>
                        ))}
                      </select>
                    </Campo>
                  </div>
                </div>

                <Campo etiqueta="Monto" htmlFor="monto">
                  <MontoInput id="monto" valor={monto} alCambiar={setMonto} />
                </Campo>

                <Campo etiqueta="Notas odontológicas" htmlFor="notas">
                  <textarea
                    id="notas"
                    className="form-control"
                    rows={4}
                    maxLength={2000}
                    placeholder="Tratamiento realizado, observaciones, indicaciones..."
                    value={notas}
                    onChange={(e) => setNotas(e.target.value)}
                  />
                  {/* al guardar, si las notas o el monto cambiaron, el backend deja constancia en la historia clínica */}
                  {editando && <small className="form-text text-muted">Si cambiás las notas o el monto, queda registrado en la historia clínica del paciente.</small>}
                </Campo>

                <div className="d-flex flex-wrap justify-content-between mt-4">
                  <div>
                    <button type="submit" className="btn btn-primary mr-2" disabled={guardando}>
                      <i className="fas fa-save mr-1"></i> {guardando ? 'Guardando...' : editando ? 'Guardar cambios' : 'Agendar turno'}
                    </button>
                    <Link to="/" className="btn btn-secondary">
                      Cancelar
                    </Link>
                  </div>
                  {editando && (
                    <button type="button" className="btn btn-outline-danger" onClick={() => setConfirmandoBorrado(true)} disabled={guardando}>
                      <i className="fas fa-trash mr-1"></i> Eliminar turno
                    </button>
                  )}
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>

      {confirmandoBorrado && turnoExistente && (
        <ConfirmDialog titulo="Eliminar turno" textoConfirmar="Eliminar" procesando={guardando} alConfirmar={eliminar} alCancelar={() => setConfirmandoBorrado(false)}>
          ¿Eliminar el turno de <strong>{apellidoYNombre({ nombre: turnoExistente.nombrePaciente, apellido: turnoExistente.apellidoPaciente })}</strong> del{' '}
          {formatearFecha(turnoExistente.fechaHora.slice(0, 10))} a las {formatearHora(parsearFechaHora(turnoExistente.fechaHora))}?
        </ConfirmDialog>
      )}
    </>
  )
}
