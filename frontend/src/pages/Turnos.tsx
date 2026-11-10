import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { ApiError } from '../api/cliente'
import { turnosApi } from '../api/turnosApi'
import Avatar from '../components/ui/Avatar'
import Cargando from '../components/ui/Cargando'
import ConfirmDialog from '../components/ui/ConfirmDialog'
import EncabezadoPagina from '../components/ui/EncabezadoPagina'
import ErrorCarga from '../components/ui/ErrorCarga'
import EstadoVacio from '../components/ui/EstadoVacio'
import Paginacion from '../components/ui/Paginacion'
import { useAvisos } from '../context/AvisosContext'
import { useConsulta } from '../hooks/useConsulta'
import type { Turno } from '../types'
import { calcularEstado } from '../utils/estadoTurno'
import { aFechaIso, esMismoDia, formatearDiaCorto, formatearFecha, formatearHora, inicioDeSemana, parsearFechaHora, sumarDias } from '../utils/fechas'
import { formatearMonto, nombreDelPaciente, normalizarTexto } from '../utils/formato'
import { armarLinkWhatsApp } from '../utils/whatsapp'

/* ----------------------------------------------------------------------------------------------------
 Filtros rápidos. Cada uno se traduce a un rango de fechas que se le pide a la API, así el servidor
 solo devuelve los turnos que interesan (no se descarga toda la historia del consultorio).
 ---------------------------------------------------------------------------------------------------- */
type Filtro = 'proximos' | 'hoy' | 'semana' | 'pasados' | 'todos'

const FILTROS: { clave: Filtro; texto: string }[] = [
  { clave: 'proximos', texto: 'Próximos' },
  { clave: 'hoy', texto: 'Hoy' },
  { clave: 'semana', texto: 'Esta semana' },
  { clave: 'pasados', texto: 'Pasados' },
  { clave: 'todos', texto: 'Todos' },
]

// undefined = "sin límite" (el parámetro no se envía y el backend usa su valor por defecto)
function rangoDelFiltro(filtro: Filtro): { desde?: string; hasta?: string } {
  const hoy = new Date()
  switch (filtro) {
    case 'proximos':
      return { desde: aFechaIso(hoy) }
    case 'hoy':
      return { desde: aFechaIso(hoy), hasta: aFechaIso(hoy) }
    case 'semana': {
      const lunes = inicioDeSemana(hoy)
      return { desde: aFechaIso(lunes), hasta: aFechaIso(sumarDias(lunes, 6)) }
    }
    case 'pasados':
      return { hasta: aFechaIso(sumarDias(hoy, -1)) }
    case 'todos':
      return {}
  }
}

/* ----------------------------------------------------------------------------------------------------
 Orden. Cada columna ordenable define su "comparador": una función que dice cuál de dos turnos va primero.
 Es la forma funcional de ordenar: en vez de escribir un if por columna, se elige la función según la columna
 y se la pasa a sort().
 ---------------------------------------------------------------------------------------------------- */
type CampoOrden = 'fecha' | 'paciente' | 'duracion' | 'monto'
type Direccion = 'asc' | 'desc'

const COMPARADORES: Record<CampoOrden, (a: Turno, b: Turno) => number> = {
  fecha: (a, b) => a.fechaHora.localeCompare(b.fechaHora), // el formato ISO ordena bien como texto
  paciente: (a, b) => nombreDelPaciente(a).localeCompare(nombreDelPaciente(b), 'es'),
  duracion: (a, b) => a.duracionMinutos - b.duracionMinutos,
  monto: (a, b) => (a.monto ?? -1) - (b.monto ?? -1), // los turnos sin monto quedan al principio al ordenar de menor a mayor
}

const TAMANIO_PAGINA = 10

// encabezado de columna con flechita que indica si está ordenada y en qué dirección
function EncabezadoOrdenable({ texto, campo, orden, direccion, alOrdenar, className = '' }: {
  texto: string
  campo: CampoOrden
  orden: CampoOrden
  direccion: Direccion
  alOrdenar: (campo: CampoOrden) => void
  className?: string
}) {
  const activo = orden === campo
  const icono = !activo ? 'fa-sort' : direccion === 'asc' ? 'fa-sort-up' : 'fa-sort-down'

  return (
    <th className={`ordenable ${className}`} onClick={() => alOrdenar(campo)} aria-sort={activo ? (direccion === 'asc' ? 'ascending' : 'descending') : 'none'}>
      {texto} <i className={`fas ${icono} ${activo ? 'text-primary' : 'text-gray-400'}`}></i>
    </th>
  )
}

export default function Turnos() {
  const { mostrarAviso } = useAvisos()

  const [filtro, setFiltro] = useState<Filtro>('proximos')
  const [busqueda, setBusqueda] = useState('')
  const [orden, setOrden] = useState<CampoOrden>('fecha')
  const [direccion, setDireccion] = useState<Direccion>('asc')
  const [pagina, setPagina] = useState(0)

  const [aEliminar, setAEliminar] = useState<Turno | null>(null)
  const [eliminando, setEliminando] = useState(false)

  const { datos, cargando, error, recargar } = useConsulta(() => {
    const { desde, hasta } = rangoDelFiltro(filtro)
    return turnosApi.listar(desde, hasta)
  }, [filtro])

  const cambiarFiltro = (nuevo: Filtro) => {
    setFiltro(nuevo)
    setPagina(0)
    // en "Pasados" lo más útil es ver primero el turno más reciente
    setOrden('fecha')
    setDireccion(nuevo === 'pasados' ? 'desc' : 'asc')
  }

  const ordenarPor = (campo: CampoOrden) => {
    // clic en la columna ya ordenada: invierte la dirección. Clic en otra: ordena por esa de menor a mayor
    if (campo === orden) setDireccion((d) => (d === 'asc' ? 'desc' : 'asc'))
    else {
      setOrden(campo)
      setDireccion('asc')
    }
    setPagina(0)
  }

  /*
   Todo el trabajo "en pantalla" (buscar, ordenar, sumar) en una sola cadena. useMemo hace que solo se repita cuando
   cambian los datos o los criterios, no en cada render.
   */
  const { filtrados, totalMonto } = useMemo(() => {
    const buscado = normalizarTexto(busqueda.trim())
    const signo = direccion === 'asc' ? 1 : -1

    const filtrados = (datos ?? [])
      // 1) buscar por paciente, teléfono o notas (sin importar tildes ni mayúsculas)
      .filter((turno) => normalizarTexto(`${nombreDelPaciente(turno)} ${turno.telefonoPaciente} ${turno.notasOdontologicas ?? ''}`).includes(buscado))
      // 2) ordenar con el comparador de la columna elegida (filter() ya devolvió una copia, así que sort() no toca los datos originales)
      .sort((a, b) => signo * COMPARADORES[orden](a, b))

    // 3) suma de los montos de los turnos filtrados (reduce acumula un único valor recorriendo la lista)
    const totalMonto = filtrados.reduce((suma, turno) => suma + (turno.monto ?? 0), 0)

    return { filtrados, totalMonto }
  }, [datos, busqueda, orden, direccion])

  // si al filtrar quedan menos páginas que la actual, se vuelve a la última que exista
  const paginaVisible = Math.min(pagina, Math.max(0, Math.ceil(filtrados.length / TAMANIO_PAGINA) - 1))
  const filas = filtrados.slice(paginaVisible * TAMANIO_PAGINA, (paginaVisible + 1) * TAMANIO_PAGINA)

  const confirmarEliminacion = async () => {
    if (!aEliminar) return
    setEliminando(true)
    try {
      await turnosApi.eliminar(aEliminar.idTurno)
      mostrarAviso(
        'exito',
        <>
          Turno de <strong>{nombreDelPaciente(aEliminar)}</strong> del {formatearFecha(aEliminar.fechaHora.slice(0, 10))} {formatearHora(parsearFechaHora(aEliminar.fechaHora))} eliminado correctamente.
        </>,
      )
      recargar()
    } catch (e) {
      mostrarAviso('error', e instanceof ApiError ? e.message : 'Ocurrió un error al eliminar el turno.')
    } finally {
      setEliminando(false)
      setAEliminar(null)
    }
  }

  const ahora = new Date()
  const propsOrden = { orden, direccion, alOrdenar: ordenarPor }

  return (
    <>
      <EncabezadoPagina titulo="Turnos">
        <Link to="/turnos/nuevo" className="btn btn-primary btn-sm shadow-sm">
          <i className="fas fa-plus fa-sm text-white-50"></i> Nuevo turno
        </Link>
      </EncabezadoPagina>

      <div className="card shadow mb-4">
        {/* barra de filtros */}
        <div className="card-header py-3">
          <div className="row align-items-center">
            <div className="col-lg-7 mb-2 mb-lg-0">
              <div className="btn-group btn-group-sm filtros-turnos" role="group" aria-label="Filtrar turnos">
                {FILTROS.map(({ clave, texto }) => (
                  <button key={clave} type="button" className={`btn ${filtro === clave ? 'btn-primary' : 'btn-outline-primary'}`} onClick={() => cambiarFiltro(clave)}>
                    {texto}
                  </button>
                ))}
              </div>
            </div>
            <div className="col-lg-5">
              <div className="input-group input-group-sm">
                <div className="input-group-prepend">
                  <span className="input-group-text bg-light border-0">
                    <i className="fas fa-search"></i>
                  </span>
                </div>
                <input
                  className="form-control bg-light border-0 small"
                  type="search"
                  placeholder="Buscar por paciente, teléfono o notas..."
                  aria-label="Buscar turnos"
                  value={busqueda}
                  onChange={(e) => {
                    setBusqueda(e.target.value)
                    setPagina(0)
                  }}
                />
              </div>
            </div>
          </div>
        </div>

        <div className="card-body">
          {cargando && !datos && <Cargando texto="Cargando turnos..." />}
          {error && <ErrorCarga mensaje={error} alReintentar={recargar} />}

          {datos && filtrados.length === 0 && (
            <EstadoVacio icono="fa-calendar-times" titulo="No hay turnos para mostrar">
              {busqueda ? 'Probá con otra búsqueda.' : 'Cambiá el filtro o agendá un turno nuevo desde la agenda.'}
            </EstadoVacio>
          )}

          {datos && filtrados.length > 0 && (
            <>
              {/* resumen de lo que se está viendo */}
              <div className="d-flex flex-wrap justify-content-between small text-gray-700 mb-3">
                <span>
                  <strong>{filtrados.length}</strong> {filtrados.length === 1 ? 'turno' : 'turnos'}
                </span>
                <span>
                  Monto total: <strong>{formatearMonto(totalMonto)}</strong>
                </span>
              </div>

              <div className="table-responsive">
                <table className="table tabla-turnos align-middle mb-0">
                  <thead>
                    <tr>
                      <EncabezadoOrdenable texto="Fecha y hora" campo="fecha" {...propsOrden} />
                      <EncabezadoOrdenable texto="Paciente" campo="paciente" {...propsOrden} />
                      <EncabezadoOrdenable texto="Duración" campo="duracion" className="d-none d-md-table-cell" {...propsOrden} />
                      <EncabezadoOrdenable texto="Monto" campo="monto" className="text-right" {...propsOrden} />
                      <th className="d-none d-xl-table-cell">Notas</th>
                      <th>Estado</th>
                      <th className="text-right">Acciones</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filas.map((turno) => {
                      const inicio = parsearFechaHora(turno.fechaHora)
                      const fin = parsearFechaHora(turno.fechaHoraFin)
                      const estado = calcularEstado(turno, ahora)

                      return (
                        <tr key={turno.idTurno} className={esMismoDia(inicio, ahora) ? 'fila-hoy' : ''}>
                          <td className="celda-fecha">
                            <div className="font-weight-bold text-gray-800 text-capitalize">{formatearDiaCorto(inicio)}</div>
                            <div className="small text-gray-600">
                              {formatearHora(inicio)} – {formatearHora(fin)}
                            </div>
                          </td>
                          <td className="celda-paciente">
                            <Link to={`/pacientes/${turno.idPaciente}`} className="d-flex align-items-center text-gray-800 enlace-paciente">
                              <Avatar nombre={turno.nombrePaciente} apellido={turno.apellidoPaciente} />
                              <span className="ml-2">
                                <span className="d-block font-weight-bold">{nombreDelPaciente(turno)}</span>
                                <span className="d-block small text-gray-600">{turno.telefonoPaciente}</span>
                              </span>
                            </Link>
                          </td>
                          <td className="d-none d-md-table-cell">
                            <span className={`badge badge-pill duracion-${turno.duracionMinutos}`}>{turno.duracionMinutos} min</span>
                          </td>
                          <td className="text-right text-nowrap celda-monto">{turno.monto !== null ? formatearMonto(turno.monto) : <span className="text-gray-400">–</span>}</td>
                          <td className="d-none d-xl-table-cell celda-notas">
                            {turno.notasOdontologicas ? <span title={turno.notasOdontologicas}>{turno.notasOdontologicas}</span> : <span className="text-gray-400">–</span>}
                          </td>
                          <td className="celda-estado">
                            <span className={`badge ${estado.claseBadge}`}>{estado.texto}</span>
                          </td>
                          <td className="text-right text-nowrap celda-acciones">
                            <Link to={`/turnos/${turno.idTurno}/editar`} className="btn btn-warning btn-circle btn-sm" title="Editar turno" aria-label="Editar turno">
                              <i className="fas fa-edit"></i>
                            </Link>{' '}
                            {/* el recordatorio solo tiene sentido si el turno todavía no pasó */}
                            {estado.clave !== 'realizado' && (
                              <>
                                <a href={armarLinkWhatsApp(turno)} target="_blank" rel="noreferrer" className="btn btn-success btn-circle btn-sm" title="Recordar por WhatsApp" aria-label="Recordar por WhatsApp">
                                  <i className="fab fa-whatsapp"></i>
                                </a>{' '}
                              </>
                            )}
                            <button className="btn btn-danger btn-circle btn-sm" title="Eliminar turno" aria-label="Eliminar turno" onClick={() => setAEliminar(turno)}>
                              <i className="fas fa-trash"></i>
                            </button>
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>

              <Paginacion pagina={paginaVisible} tamanio={TAMANIO_PAGINA} total={filtrados.length} alCambiar={setPagina} />
            </>
          )}
        </div>
      </div>

      {aEliminar && (
        <ConfirmDialog titulo="Eliminar turno" textoConfirmar="Eliminar" procesando={eliminando} alConfirmar={confirmarEliminacion} alCancelar={() => setAEliminar(null)}>
          ¿Eliminar el turno de <strong>{nombreDelPaciente(aEliminar)}</strong> del {formatearFecha(aEliminar.fechaHora.slice(0, 10))} a las{' '}
          {formatearHora(parsearFechaHora(aEliminar.fechaHora))}?
        </ConfirmDialog>
      )}
    </>
  )
}
