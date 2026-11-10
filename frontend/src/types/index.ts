/*
 Tipos que describen EXACTAMENTE el JSON que devuelve / recibe la API (son el espejo de los DTOs del backend).
 Si cambia un DTO en Java, se cambia el tipo acá y TypeScript marca todos los lugares que hay que corregir.
 */

// "YYYY-MM-DD"
export type FechaIso = string
// "YYYY-MM-DDTHH:mm:ss" (sin zona horaria: es la hora de pared del consultorio)
export type FechaHoraIso = string

export interface PacienteResumen {
  idPaciente: number
  nombre: string
  apellido: string
  dni: string
  telefono: string
  direccion: string
  fechaNacimiento: FechaIso
}

// el detalle de un paciente suma la historia clínica (en las listas no viaja porque puede ser muy larga)
export interface Paciente extends PacienteResumen {
  historiaClinica: string | null
}

// lo que se envía para crear o editar un paciente (PacienteRequestDTO)
export type PacienteRequest = Omit<PacienteResumen, 'idPaciente'>

export interface Turno {
  idTurno: number
  fechaHora: FechaHoraIso
  fechaHoraFin: FechaHoraIso
  duracionMinutos: number
  monto: number | null
  notasOdontologicas: string | null
  idPaciente: number
  nombrePaciente: string
  apellidoPaciente: string
  telefonoPaciente: string
}

// lo que se envía para crear o editar un turno (TurnoRequestDTO)
export interface TurnoRequest {
  fechaHora: string
  duracionMinutos: number
  idPaciente: number
  monto: number | null
  notasOdontologicas: string | null
}

export type DiaSemana = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY'

// horario del consultorio, lo define el backend (GET /api/agenda/configuracion)
export interface AgendaConfiguracion {
  horaInicio: string // "08:00:00"
  horaFin: string // "12:30:00"
  diasLaborales: DiaSemana[]
  duracionesPermitidas: number[]
  intervaloMinutos: number
}

export interface Resumen {
  turnosHoy: number
  turnosSemana: number
  totalPacientes: number
  proximoTurno: Turno | null
}

// quién está logueado (respuesta de /api/auth/login y /api/auth/me)
export interface UsuarioActual {
  idUsuario: number
  nombreUsuario: string
}

// formato único de error de la API (RespuestaErrorDTO)
export interface RespuestaError {
  status: number
  error: string
  message: string
}
