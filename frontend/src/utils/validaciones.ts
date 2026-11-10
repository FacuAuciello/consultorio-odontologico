import type { PacienteRequest } from '../types'
import { esFechaValida } from './fechas'

/*
 Validaciones del formulario de pacientes. Reflejan las del backend (PacienteRequestDTO) para avisar al instante,
 sin esperar la respuesta del servidor. El backend SIEMPRE vuelve a validar: la validación del front es solo comodidad,
 no seguridad (cualquiera puede llamar a la API sin pasar por la pantalla).
 */

// una letra y luego letras, espacios, punto, apóstrofe o guion: "De la Torre", "O'Connor", "Ana-María"
const REGEX_NOMBRE = /^\p{L}[\p{L} .'-]*$/u

export type ErroresPaciente = Partial<Record<keyof PacienteRequest, string>>

export function validarPaciente(datos: PacienteRequest): ErroresPaciente {
  const errores: ErroresPaciente = {}

  if (!datos.nombre.trim()) errores.nombre = 'El nombre es obligatorio'
  else if (!REGEX_NOMBRE.test(datos.nombre.trim())) errores.nombre = 'El nombre solo puede contener letras y espacios'

  if (!datos.apellido.trim()) errores.apellido = 'El apellido es obligatorio'
  else if (!REGEX_NOMBRE.test(datos.apellido.trim())) errores.apellido = 'El apellido solo puede contener letras y espacios'

  if (!datos.dni.trim()) errores.dni = 'El DNI es obligatorio'
  else if (!/^\d{6,9}$/.test(datos.dni.trim())) errores.dni = 'El DNI debe tener entre 6 y 9 números, sin puntos'

  if (!datos.telefono.trim()) errores.telefono = 'El teléfono es obligatorio'
  else if (!/^[0-9+()\s-]{6,25}$/.test(datos.telefono.trim())) errores.telefono = 'El teléfono solo puede contener números, espacios, + y -'

  if (!datos.direccion.trim()) errores.direccion = 'La dirección es obligatoria'

  if (!datos.fechaNacimiento) errores.fechaNacimiento = 'La fecha de nacimiento es obligatoria (día, mes y año)'

  return errores
}

/*
 Valida los tres campos de la fecha de nacimiento (día, mes, año) y devuelve el mensaje de error, o null si está todo bien.
 Se hace campo por campo para dar el mensaje más útil ("el mes debe estar entre 1 y 12") en vez de un genérico.
 */
export function validarFechaNacimiento(dia: string, mes: string, anio: string, hoy: Date = new Date()): string | null {
  if (!dia || !mes || !anio) return 'La fecha de nacimiento es obligatoria (día, mes y año)'

  const d = Number(dia)
  const m = Number(mes)
  const a = Number(anio)
  const anioActual = hoy.getFullYear()

  if (d < 1 || d > 31) return 'El día de nacimiento debe estar entre 1 y 31'
  if (m < 1 || m > 12) return 'El mes de nacimiento debe estar entre 1 y 12'
  if (anio.length !== 4 || a < 1900 || a > anioActual) return `El año de nacimiento debe estar entre 1900 y ${anioActual}`
  if (!esFechaValida(d, m, a)) return 'La fecha de nacimiento no es válida (revisá el día y el mes)'
  if (new Date(a, m - 1, d) >= new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate())) {
    return 'La fecha de nacimiento tiene que ser anterior a hoy'
  }
  return null
}
