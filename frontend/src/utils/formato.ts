import type { Turno } from '../types'

/*
 Utilidades para mostrar datos.
 */

// 15000 -> "$ 15.000"  (Intl.NumberFormat con locale es-AR: punto de miles, coma decimal)
export function formatearMonto(monto: number): string {
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS', maximumFractionDigits: 0 })
    .format(monto)
    .replace(/\s/g, ' ') // el espacio que devuelve Intl es "no separable": se cambia por uno normal
}

// 15000 -> "15.000" (sin signo $, para usar dentro de un input)
export function formatearConPuntos(numero: number): string {
  return new Intl.NumberFormat('es-AR', { maximumFractionDigits: 0 }).format(numero)
}

// "15.000" -> 15000 | "" -> null. Se descartan todos los caracteres que no sean dígitos
export function desformatearNumero(texto: string): number | null {
  const soloDigitos = texto.replace(/\D/g, '')
  return soloDigitos === '' ? null : Number(soloDigitos)
}

// ("Lucía", "Fernández") -> "LF"  (para el círculo con iniciales)
export function iniciales(nombre: string, apellido: string): string {
  return `${nombre.charAt(0)}${apellido.charAt(0)}`.toUpperCase()
}

// "Fernández, Lucía"  (apellido primero: así ordena la agenda y las listas)
export function apellidoYNombre(persona: { nombre: string; apellido: string }): string {
  return `${persona.apellido}, ${persona.nombre}`
}

export function nombreDelPaciente(turno: Turno): string {
  return `${turno.apellidoPaciente}, ${turno.nombrePaciente}`
}

// quita tildes y pasa a minúsculas: "Pérez" y "perez" se consideran iguales al buscar
export function normalizarTexto(texto: string): string {
  return texto.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()
}
