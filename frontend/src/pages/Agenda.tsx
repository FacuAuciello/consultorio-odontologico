import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import esLocale from '@fullcalendar/core/locales/es'
import type { DatesSetArg, EventClickArg, EventContentArg, EventInput } from '@fullcalendar/core'
import interactionPlugin, { type DateClickArg } from '@fullcalendar/interaction'
import FullCalendar from '@fullcalendar/react'
import timeGridPlugin from '@fullcalendar/timegrid'
import { agendaApi } from '../api/agendaApi'
import { turnosApi } from '../api/turnosApi'
import EncabezadoPagina from '../components/ui/EncabezadoPagina'
import Cargando from '../components/ui/Cargando'
import ErrorCarga from '../components/ui/ErrorCarga'
import { useConsulta } from '../hooks/useConsulta'
import type { AgendaConfiguracion, Resumen, Turno } from '../types'
import { calcularEstado } from '../utils/estadoTurno'
import { aFechaHoraIso, aFechaIso, formatearDiaCorto, formatearHora, inicioDeSemana, numeroDeDia, parsearFechaHora, recortarHora, sumarDias } from '../utils/fechas'
import { nombreDelPaciente } from '../utils/formato'

/*
 Tarjeta de número del panel superior (estilo "Earnings" de SB Admin 2).
 */
function TarjetaResumen({ titulo, valor, icono, color }: { titulo: string; valor: string; icono: string; color: string }) {
  return (
    <div className="col-xl-3 col-sm-6 mb-4">
      <div className={`card border-left-${color} shadow h-100 py-2`}>
        <div className="card-body">
          <div className="row no-gutters align-items-center">
            <div className="col mr-2">
              <div className={`text-xs font-weight-bold text-${color} text-uppercase mb-1`}>{titulo}</div>
              <div className="h5 mb-0 font-weight-bold text-gray-800 texto-resumen">{valor}</div>
            </div>
            <div className="col-auto">
              <i className={`fas ${icono} fa-2x text-gray-300`}></i>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

function textoProximoTurno(resumen: Resumen | null): string {
  const turno = resumen?.proximoTurno
  if (!turno) return 'Sin turnos próximos'

  const inicio = parsearFechaHora(turno.fechaHora)
  return `${formatearDiaCorto(inicio)} ${formatearHora(inicio)} · ${nombreDelPaciente(turno)}`
}

/*
 Cómo se dibuja cada turno dentro del calendario. FullCalendar llama a esta función por cada evento.
 Un turno de 30 minutos ocupa un solo casillero (cabe una línea); el de 1 hora tiene lugar para dos.
 */
function ContenidoEvento({ info }: { info: EventContentArg }) {
  const turno = info.event.extendedProps.turno as Turno
  const inicio = parsearFechaHora(turno.fechaHora)
  const fin = parsearFechaHora(turno.fechaHoraFin)

  if (turno.duracionMinutos <= 30) {
    return (
      <div className="evento-turno evento-una-linea" title={nombreDelPaciente(turno)}>
        <span className="evento-hora">{formatearHora(inicio)}</span> {nombreDelPaciente(turno)}
      </div>
    )
  }

  return (
    <div className="evento-turno" title={nombreDelPaciente(turno)}>
      <div className="evento-hora">
        {formatearHora(inicio)} – {formatearHora(fin)}
      </div>
      <div className="evento-nombre">{nombreDelPaciente(turno)}</div>
    </div>
  )
}

// el calendario se dibuja recién cuando ya se conoce el horario del consultorio (viene del backend)
function Calendario({ config }: { config: AgendaConfiguracion }) {
  const navigate = useNavigate()

  /*
   Rango de fechas visible en el calendario. Arranca con la semana actual y FullCalendar lo actualiza
   (evento datesSet) cada vez que el usuario navega a otra semana o cambia entre vista Día / Semana.
   Son dos strings separados (y no un objeto) para que React no vuelva a consultar si el rango es el mismo.
   */
  const semanaActual = inicioDeSemana(new Date())
  const [desde, setDesde] = useState(aFechaIso(semanaActual))
  const [hasta, setHasta] = useState(aFechaIso(sumarDias(semanaActual, 6)))

  // se consultan SOLO los turnos del rango visible, no toda la tabla
  const { datos: turnos, error, recargar } = useConsulta(() => turnosApi.listar(desde, hasta), [desde, hasta])

  // los datos de la API se convierten al formato que entiende FullCalendar. useMemo: solo se recalcula si cambian los turnos
  const eventos = useMemo<EventInput[]>(() => {
    const ahora = new Date()
    return (turnos ?? []).map((turno) => ({
      id: String(turno.idTurno),
      title: nombreDelPaciente(turno), // FullCalendar exige un title (se usa en accesibilidad); el texto visible lo arma ContenidoEvento
      start: turno.fechaHora,
      end: turno.fechaHoraFin,
      // clases CSS según duración y estado: el color de cada turno se define en app.css
      classNames: [`evento-${turno.duracionMinutos}`, calcularEstado(turno, ahora).clave === 'realizado' ? 'evento-realizado' : ''],
      extendedProps: { turno }, // se "pega" el turno completo al evento para leerlo después en ContenidoEvento
    }))
  }, [turnos])

  const manejarCambioDeFechas = (info: DatesSetArg) => {
    // info.end es EXCLUSIVO (el día siguiente al último visible), la API espera el último día inclusive
    setDesde(aFechaIso(info.start))
    setHasta(aFechaIso(sumarDias(info.end, -1)))
  }

  // clic en un casillero vacío -> nuevo turno con esa fecha y hora ya cargadas
  const manejarClicEnCasillero = (info: DateClickArg) => {
    navigate(`/turnos/nuevo?fechaHora=${encodeURIComponent(aFechaHoraIso(info.date))}`)
  }

  const manejarClicEnTurno = (info: EventClickArg) => {
    navigate(`/turnos/${info.event.id}/editar`)
  }

  // FullCalendar numera los días 0 (domingo) a 6 (sábado); se ocultan los que el consultorio no atiende
  const diasLaborales = config.diasLaborales.map(numeroDeDia)
  const diasOcultos = [0, 1, 2, 3, 4, 5, 6].filter((dia) => !diasLaborales.includes(dia))

  return (
    <div className="card shadow mb-4">
      <div className="card-header py-3 d-flex flex-wrap align-items-center justify-content-between">
        <h6 className="m-0 font-weight-bold text-primary">Semana actual</h6>

        {/* referencia de colores de la agenda */}
        <div className="leyenda-agenda small text-gray-600">
          <span><i className="fas fa-circle leyenda-30"></i> 30 min</span>
          <span><i className="fas fa-circle leyenda-60"></i> 1 hora</span>
          <span><i className="fas fa-circle leyenda-realizado"></i> Realizado</span>
        </div>
      </div>

      <div className="card-body p-0">
        {error && <ErrorCarga mensaje={error} alReintentar={recargar} />}

        <div id="calendario">
          <FullCalendar
            plugins={[timeGridPlugin, interactionPlugin]}
            locales={[esLocale]}
            locale="es"
            // en celulares se arranca en vista de un solo día (una semana entera no entra en la pantalla)
            initialView={window.innerWidth < 768 ? 'timeGridDay' : 'timeGridWeek'}
            headerToolbar={{ left: 'prev,next today', center: 'title', right: 'timeGridDay,timeGridWeek' }}
            // horario, días y tamaño de los casilleros: todo sale de la configuración del backend
            slotMinTime={config.horaInicio}
            slotMaxTime={config.horaFin}
            slotDuration={{ minutes: config.intervaloMinutos }}
            slotLabelInterval={{ minutes: config.intervaloMinutos }}
            slotLabelFormat={{ hour: '2-digit', minute: '2-digit', hour12: false }}
            hiddenDays={diasOcultos}
            allDaySlot={false}
            nowIndicator
            expandRows
            height="auto"
            dayHeaderFormat={{ weekday: 'long', day: 'numeric', month: 'short' }}
            events={eventos}
            eventContent={(info) => <ContenidoEvento info={info} />}
            dateClick={manejarClicEnCasillero}
            eventClick={manejarClicEnTurno}
            datesSet={manejarCambioDeFechas}
            // evita que el calendario seleccione texto al hacer doble clic
            selectable={false}
          />
        </div>
      </div>
    </div>
  )
}

export default function Agenda() {
  const configuracion = useConsulta(agendaApi.configuracion, [])
  const resumen = useConsulta(agendaApi.resumen, [])

  return (
    <>
      <EncabezadoPagina titulo="Agenda" />

      {/* tarjetas de resumen: si todavía no llegaron los números se muestra un guion, la agenda no espera por ellas */}
      <div className="row">
        <TarjetaResumen titulo="Turnos de hoy" valor={String(resumen.datos?.turnosHoy ?? '–')} icono="fa-calendar-day" color="primary" />
        <TarjetaResumen titulo="Turnos de la semana" valor={String(resumen.datos?.turnosSemana ?? '–')} icono="fa-calendar-week" color="success" />
        <TarjetaResumen titulo="Pacientes registrados" valor={String(resumen.datos?.totalPacientes ?? '–')} icono="fa-users" color="info" />
        <TarjetaResumen titulo="Próximo turno" valor={textoProximoTurno(resumen.datos)} icono="fa-clock" color="warning" />
      </div>

      {configuracion.cargando && <Cargando texto="Cargando agenda..." />}
      {configuracion.error && <ErrorCarga mensaje={configuracion.error} alReintentar={configuracion.recargar} />}
      {configuracion.datos && (
        // recortarHora: "08:00:00" -> "08:00". El key fuerza a recrear el calendario si cambia el horario
        <Calendario key={`${recortarHora(configuracion.datos.horaInicio)}-${recortarHora(configuracion.datos.horaFin)}`} config={configuracion.datos} />
      )}
    </>
  )
}
