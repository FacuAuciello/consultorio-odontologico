import type { Turno, TurnoRequest } from '../types'
import { peticion } from './cliente'

export const turnosApi = {
  // desde / hasta: "YYYY-MM-DD", ambos inclusivos y opcionales
  listar: (desde?: string, hasta?: string) => peticion<Turno[]>('GET', '/api/turnos', { params: { desde, hasta } }),

  obtener: (idTurno: number) => peticion<Turno>('GET', `/api/turnos/${idTurno}`),

  crear: (datos: TurnoRequest) => peticion<Turno>('POST', '/api/turnos', { cuerpo: datos }),

  editar: (idTurno: number, datos: TurnoRequest) => peticion<Turno>('PUT', `/api/turnos/${idTurno}`, { cuerpo: datos }),

  eliminar: (idTurno: number) => peticion<void>('DELETE', `/api/turnos/${idTurno}`),
}
