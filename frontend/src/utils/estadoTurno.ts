import type { Turno } from '../types'
import { esMismoDia, parsearFechaHora } from './fechas'

/*
 El estado de un turno NO se guarda en la base: se deduce comparando su horario con el momento actual.
 Así nunca queda desactualizado (nadie tiene que acordarse de marcar "realizado").
 */
export type EstadoTurno = 'realizado' | 'en-curso' | 'hoy' | 'proximo'

export interface InfoEstado {
  clave: EstadoTurno
  texto: string
  claseBadge: string // clases de Bootstrap para el color
}

const ESTADOS: Record<EstadoTurno, InfoEstado> = {
  realizado: { clave: 'realizado', texto: 'Realizado', claseBadge: 'badge-secondary' },
  'en-curso': { clave: 'en-curso', texto: 'En curso', claseBadge: 'badge-warning' },
  hoy: { clave: 'hoy', texto: 'Hoy', claseBadge: 'badge-info' },
  proximo: { clave: 'proximo', texto: 'Próximo', claseBadge: 'badge-primary' },
}

// "ahora" se recibe por parámetro (en vez de llamar a new Date() adentro) para poder testear con una fecha fija
export function calcularEstado(turno: Turno, ahora: Date = new Date()): InfoEstado {
  const inicio = parsearFechaHora(turno.fechaHora)
  const fin = parsearFechaHora(turno.fechaHoraFin)

  if (fin <= ahora) return ESTADOS.realizado
  if (inicio <= ahora) return ESTADOS['en-curso']
  if (esMismoDia(inicio, ahora)) return ESTADOS.hoy
  return ESTADOS.proximo
}
