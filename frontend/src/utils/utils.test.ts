import { describe, expect, it } from 'vitest'
import type { AgendaConfiguracion, Turno } from '../types'
import { calcularEdad, esFechaValida, generarHorarios, inicioDeSemana, aFechaHoraIso, parsearFecha, aFechaIso } from './fechas'
import { desformatearNumero, formatearMonto, normalizarTexto } from './formato'
import { calcularEstado } from './estadoTurno'
import { normalizarTelefono } from './whatsapp'
import { validarFechaNacimiento, validarPaciente } from './validaciones'

const CONFIG: AgendaConfiguracion = {
  horaInicio: '08:00:00',
  horaFin: '12:30:00',
  diasLaborales: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'],
  duracionesPermitidas: [30, 60],
  intervaloMinutos: 30,
}

describe('fechas', () => {
  it('arma el texto de fecha y hora en hora LOCAL (sin pasar por UTC)', () => {
    const fecha = new Date(2026, 9, 26, 22, 30) // 26/10/2026 22:30
    expect(aFechaHoraIso(fecha)).toBe('2026-10-26T22:30')
  })

  it('parsea una fecha sin correrla un día por la zona horaria', () => {
    expect(aFechaIso(parsearFecha('1990-04-12'))).toBe('1990-04-12')
  })

  it('detecta fechas que no existen en el calendario', () => {
    expect(esFechaValida(31, 2, 2026)).toBe(false)
    expect(esFechaValida(29, 2, 2024)).toBe(true) // bisiesto
    expect(esFechaValida(29, 2, 2025)).toBe(false)
  })

  it('calcula la edad según el cumpleaños', () => {
    const hoy = new Date(2026, 9, 29)
    expect(calcularEdad('1990-10-29', hoy)).toBe(36) // cumple hoy
    expect(calcularEdad('1990-10-30', hoy)).toBe(35) // cumple mañana
  })

  it('el inicio de semana es siempre lunes', () => {
    expect(aFechaIso(inicioDeSemana(new Date(2026, 9, 29)))).toBe('2026-10-26') // jueves -> lunes
    expect(aFechaIso(inicioDeSemana(new Date(2026, 10, 1)))).toBe('2026-10-26') // domingo -> lunes anterior
    expect(aFechaIso(inicioDeSemana(new Date(2026, 9, 26)))).toBe('2026-10-26') // lunes -> el mismo
  })

  it('genera los horarios de la agenda según la duración', () => {
    expect(generarHorarios(CONFIG, 30)).toEqual(['08:00', '08:30', '09:00', '09:30', '10:00', '10:30', '11:00', '11:30', '12:00'])
    // con 1 hora el último turno posible empieza 11:30 (termina 12:30)
    expect(generarHorarios(CONFIG, 60).at(-1)).toBe('11:30')
  })
})

describe('formato', () => {
  it('formatea montos en pesos argentinos', () => {
    expect(formatearMonto(15000)).toBe('$ 15.000')
  })

  it('convierte el texto de un input a número', () => {
    expect(desformatearNumero('15.000')).toBe(15000)
    expect(desformatearNumero('abc')).toBeNull()
    expect(desformatearNumero('')).toBeNull()
  })

  it('normaliza texto para buscar sin importar tildes ni mayúsculas', () => {
    expect(normalizarTexto('Pérez')).toBe('perez')
  })
})

describe('estado del turno', () => {
  const turno = (inicio: string, fin: string): Turno => ({
    idTurno: 1, fechaHora: inicio, fechaHoraFin: fin, duracionMinutos: 30, monto: null, notasOdontologicas: null,
    idPaciente: 1, nombrePaciente: 'Ana', apellidoPaciente: 'Pérez', telefonoPaciente: '2235550101',
  })
  const ahora = new Date(2026, 9, 29, 10, 0) // jueves 29/10/2026 10:00

  it('un turno que ya terminó está realizado', () => {
    expect(calcularEstado(turno('2026-10-29T09:00:00', '2026-10-29T09:30:00'), ahora).clave).toBe('realizado')
  })
  it('un turno que está transcurriendo está en curso', () => {
    expect(calcularEstado(turno('2026-10-29T09:45:00', '2026-10-29T10:15:00'), ahora).clave).toBe('en-curso')
  })
  it('un turno de más tarde el mismo día es "hoy"', () => {
    expect(calcularEstado(turno('2026-10-29T11:00:00', '2026-10-29T11:30:00'), ahora).clave).toBe('hoy')
  })
  it('un turno de otro día futuro es próximo', () => {
    expect(calcularEstado(turno('2026-10-30T09:00:00', '2026-10-30T09:30:00'), ahora).clave).toBe('proximo')
  })
})

describe('whatsapp', () => {
  it('deja el teléfono solo con dígitos y el código de país', () => {
    expect(normalizarTelefono('223 555-0101')).toBe('542235550101')
    expect(normalizarTelefono('+54 223 555-0101')).toBe('542235550101')
    expect(normalizarTelefono('0223 555-0101')).toBe('542235550101')
  })
})

describe('validaciones', () => {
  const hoy = new Date(2026, 9, 29)

  it('acepta una fecha de nacimiento correcta', () => {
    expect(validarFechaNacimiento('12', '04', '1990', hoy)).toBeNull()
  })
  it('rechaza el 31 de febrero', () => {
    expect(validarFechaNacimiento('31', '02', '1990', hoy)).toMatch(/no es válida/)
  })
  it('rechaza un mes fuera de rango', () => {
    expect(validarFechaNacimiento('10', '13', '1990', hoy)).toMatch(/mes/)
  })
  it('rechaza fechas futuras', () => {
    expect(validarFechaNacimiento('01', '01', '2026', hoy)).toBeNull() // ya pasó
    expect(validarFechaNacimiento('30', '10', '2026', hoy)).toMatch(/anterior a hoy/)
  })

  it('exige los datos obligatorios del paciente', () => {
    const errores = validarPaciente({ nombre: '', apellido: '', dni: '', telefono: '', direccion: '', fechaNacimiento: '' })
    expect(Object.keys(errores)).toHaveLength(6)
  })
  it('acepta apellidos con espacios y apóstrofes', () => {
    const errores = validarPaciente({
      nombre: 'Ana María', apellido: "O'Connor de la Torre", dni: '30123456', telefono: '223 555-0101',
      direccion: 'Calle 123', fechaNacimiento: '1990-04-12',
    })
    expect(errores).toEqual({})
  })
})
