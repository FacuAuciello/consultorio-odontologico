import type { AgendaConfiguracion, Resumen } from '../types'
import { peticion } from './cliente'

export const agendaApi = {
  configuracion: () => peticion<AgendaConfiguracion>('GET', '/api/agenda/configuracion'),
  resumen: () => peticion<Resumen>('GET', '/api/agenda/resumen'),
}
