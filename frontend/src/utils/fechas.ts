import type { AgendaConfiguracion, DiaSemana, FechaHoraIso, FechaIso } from '../types'

/*
 Utilidades de fechas.

 POR QUÉ NO SE USA toISOString(): esa función convierte a UTC. En Argentina (UTC-3) un turno de las 22:00
 se "movería" al día siguiente. La API trabaja con la hora local del consultorio, así que se arman los textos
 a mano con los componentes locales (getFullYear, getMonth, ...).
 */

const dosDigitos = (n: number): string => String(n).padStart(2, '0')

// Date -> "2026-10-26"
export function aFechaIso(fecha: Date): FechaIso {
  return `${fecha.getFullYear()}-${dosDigitos(fecha.getMonth() + 1)}-${dosDigitos(fecha.getDate())}`
}

// Date -> "2026-10-26T09:30" (formato que acepta el backend para LocalDateTime)
export function aFechaHoraIso(fecha: Date): string {
  return `${aFechaIso(fecha)}T${dosDigitos(fecha.getHours())}:${dosDigitos(fecha.getMinutes())}`
}

/*
 "2026-10-26T09:30:00" -> Date en hora local.
 Un texto con hora y SIN zona se interpreta como hora local (así lo define la especificación de JavaScript).
 */
export function parsearFechaHora(texto: FechaHoraIso): Date {
  return new Date(texto)
}

/*
 "1990-04-12" -> Date en hora local.
 Ojo: new Date("1990-04-12") (solo fecha) se interpreta como UTC y en Argentina daría el 11 de abril a las 21:00.
 Por eso se construye con año, mes y día separados.
 */
export function parsearFecha(texto: FechaIso): Date {
  const [anio, mes, dia] = texto.split('-').map(Number)
  return new Date(anio, mes - 1, dia)
}

// "2026-10-26" -> "26/10/2026"
export function formatearFecha(texto: FechaIso): string {
  const [anio, mes, dia] = texto.split('-')
  return `${dia}/${mes}/${anio}`
}

// Date -> "09:30"
export function formatearHora(fecha: Date): string {
  return `${dosDigitos(fecha.getHours())}:${dosDigitos(fecha.getMinutes())}`
}

// Date -> "lun 26 oct"
export function formatearDiaCorto(fecha: Date): string {
  return new Intl.DateTimeFormat('es-AR', { weekday: 'short', day: 'numeric', month: 'short' })
    .format(fecha)
    .replace(/\./g, '')
}

// Date -> "lunes 26 de octubre de 2026"
export function formatearDiaLargo(fecha: Date): string {
  return new Intl.DateTimeFormat('es-AR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).format(fecha)
}

// ¿Es el mismo día calendario? (se comparan año, mes y día, no los milisegundos)
export function esMismoDia(a: Date, b: Date): boolean {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate()
}

export function sumarDias(fecha: Date, dias: number): Date {
  const copia = new Date(fecha)
  copia.setDate(copia.getDate() + dias)
  return copia
}

// lunes de la semana de la fecha (la semana del consultorio empieza el lunes)
export function inicioDeSemana(fecha: Date): Date {
  const copia = new Date(fecha.getFullYear(), fecha.getMonth(), fecha.getDate())
  const diasDesdeLunes = (copia.getDay() + 6) % 7 // domingo(0) -> 6, lunes(1) -> 0, martes(2) -> 1 ...
  return sumarDias(copia, -diasDesdeLunes)
}

/*
 ¿Existe esa fecha en el calendario? Sirve para detectar 31/02 o 30/04.
 Truco: se crea el Date y se comprueba que JavaScript no haya "corrido" la fecha (new Date(2026, 1, 31) da el 3 de marzo).
 */
export function esFechaValida(dia: number, mes: number, anio: number): boolean {
  const fecha = new Date(anio, mes - 1, dia)
  return fecha.getFullYear() === anio && fecha.getMonth() === mes - 1 && fecha.getDate() === dia
}

// años cumplidos a una fecha de referencia
export function calcularEdad(nacimiento: FechaIso, hoy: Date = new Date()): number {
  const fecha = parsearFecha(nacimiento)
  let edad = hoy.getFullYear() - fecha.getFullYear()
  // si todavía no llegó el cumpleaños de este año, falta restar uno
  const yaCumplio = hoy.getMonth() > fecha.getMonth() || (hoy.getMonth() === fecha.getMonth() && hoy.getDate() >= fecha.getDate())
  if (!yaCumplio) edad--
  return edad
}

// ---------------------------------------- horario de la agenda ----------------------------------------

// "08:00:00" -> "08:00"  (el backend manda los segundos, a la pantalla no le interesan)
export function recortarHora(hora: string): string {
  return hora.slice(0, 5)
}

// "09:30" -> 570 (minutos desde la medianoche). Con minutos es fácil sumar y comparar horarios
export function aMinutos(hora: string): number {
  const [h, m] = hora.split(':').map(Number)
  return h * 60 + m
}

// 570 -> "09:30"
export function deMinutos(minutos: number): string {
  return `${dosDigitos(Math.floor(minutos / 60))}:${dosDigitos(minutos % 60)}`
}

/*
 Horarios de inicio posibles para un turno de cierta duración: "08:00", "08:30", ...
 Solo entran los que TERMINAN antes de que cierre el consultorio (con 60 min, el último inicio es 11:30 si se cierra a las 12:30)
 */
export function generarHorarios(config: AgendaConfiguracion, duracionMinutos: number): string[] {
  const apertura = aMinutos(config.horaInicio)
  const cierre = aMinutos(config.horaFin)
  const horarios: string[] = []

  for (let inicio = apertura; inicio + duracionMinutos <= cierre; inicio += config.intervaloMinutos) {
    horarios.push(deMinutos(inicio))
  }
  return horarios
}

// el backend usa nombres de java.time.DayOfWeek; getDay() de JavaScript va de domingo(0) a sábado(6)
const NUMERO_DE_DIA: Record<DiaSemana, number> = {
  SUNDAY: 0,
  MONDAY: 1,
  TUESDAY: 2,
  WEDNESDAY: 3,
  THURSDAY: 4,
  FRIDAY: 5,
  SATURDAY: 6,
}

export function numeroDeDia(dia: DiaSemana): number {
  return NUMERO_DE_DIA[dia]
}

// ¿el consultorio atiende ese día de la semana?
export function esDiaLaboral(fecha: Date, config: AgendaConfiguracion): boolean {
  return config.diasLaborales.some((dia) => numeroDeDia(dia) === fecha.getDay())
}
