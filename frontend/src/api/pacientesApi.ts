import type { Paciente, PacienteRequest, PacienteResumen, Turno } from '../types'
import { peticion } from './cliente'

// una función por endpoint del backend (PacienteController). Los componentes nunca arman URLs a mano
export const pacientesApi = {
  listar: (busqueda?: string) => peticion<PacienteResumen[]>('GET', '/api/pacientes', { params: { busqueda } }),

  obtener: (idPaciente: number) => peticion<Paciente>('GET', `/api/pacientes/${idPaciente}`),

  turnosDelPaciente: (idPaciente: number) => peticion<Turno[]>('GET', `/api/pacientes/${idPaciente}/turnos`),

  crear: (datos: PacienteRequest) => peticion<Paciente>('POST', '/api/pacientes', { cuerpo: datos }),

  editar: (idPaciente: number, datos: PacienteRequest) =>
    peticion<Paciente>('PUT', `/api/pacientes/${idPaciente}`, { cuerpo: datos }),

  // PATCH: modifica solo la historia clínica
  actualizarHistoriaClinica: (idPaciente: number, historiaClinica: string) =>
    peticion<Paciente>('PATCH', `/api/pacientes/${idPaciente}/historia-clinica`, { cuerpo: { historiaClinica } }),

  eliminar: (idPaciente: number) => peticion<void>('DELETE', `/api/pacientes/${idPaciente}`),
}
