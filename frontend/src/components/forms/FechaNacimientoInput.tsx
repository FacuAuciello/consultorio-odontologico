import { useEffect, useRef, useState } from 'react'
import type { FechaIso } from '../../types'
import { validarFechaNacimiento } from '../../utils/validaciones'

interface Props {
  id: string
  // fecha completa en formato "YYYY-MM-DD" o "" si todavía no está completa / es inválida
  valor: FechaIso
  /*
   Se llama cada vez que el usuario toca un campo. Devuelve la fecha ya armada ("" si no es válida)
   y el mensaje de error (null si está todo bien). Así el formulario padre solo guarda dos datos
   y no necesita conocer los tres campos internos.
   */
  alCambiar: (fecha: FechaIso, error: string | null) => void
  hayError?: boolean
}

// "2026-4-5" -> "2026-04-05"
const dosDigitos = (n: string) => n.padStart(2, '0')

/*
 Tres casilleros (DD / MM / AAAA) en vez de un calendario desplegable: para fechas de nacimiento es mucho más rápido
 tipear "12" "04" "1990" que navegar un calendario hasta el año 1990.
 Dificultades que resuelve este componente:
   1) auto-avance: al completar el día salta al mes, y al completar el mes salta al año
   2) retroceso: Backspace en un casillero vacío vuelve al anterior
   3) mantiene su propio texto mientras se escribe (la fecha está "incompleta") y solo avisa al padre una fecha completa y válida
 */
export default function FechaNacimientoInput({ id, valor, alCambiar, hayError = false }: Props) {
  const [anio0, mes0, dia0] = valor ? valor.split('-') : ['', '', '']

  const [dia, setDia] = useState(dia0)
  const [mes, setMes] = useState(mes0)
  const [anio, setAnio] = useState(anio0)

  // refs a los tres <input> para poder mover el foco con código
  const refDia = useRef<HTMLInputElement>(null)
  const refMes = useRef<HTMLInputElement>(null)
  const refAnio = useRef<HTMLInputElement>(null)

  /*
   Sincroniza con el valor que llega por props. Se necesita porque al EDITAR un paciente, el formulario recibe la fecha
   después de que el componente ya se dibujó (cuando termina la carga). Solo se pisa el texto interno si la fecha que llega
   es distinta de la que ya está escrita; si no, se sobreescribiría lo que el usuario está tipeando.
   */
  useEffect(() => {
    if (valor && valor !== `${anio}-${dosDigitos(mes)}-${dosDigitos(dia)}`) {
      const [a, m, d] = valor.split('-')
      setAnio(a)
      setMes(m)
      setDia(d)
    }
    // solo interesa reaccionar cuando cambia "valor" desde afuera
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [valor])

  // avisa al padre con el estado actual de los tres casilleros
  const notificar = (d: string, m: string, a: string) => {
    const error = validarFechaNacimiento(d, m, a)
    alCambiar(error === null ? `${a}-${dosDigitos(m)}-${dosDigitos(d)}` : '', error)
  }

  // deja solo dígitos y recorta al largo máximo del casillero
  const limpiar = (texto: string, largo: number) => texto.replace(/\D/g, '').slice(0, largo)

  const cambiarDia = (texto: string) => {
    const nuevo = limpiar(texto, 2)
    setDia(nuevo)
    notificar(nuevo, mes, anio)
    if (nuevo.length === 2) refMes.current?.focus() // auto-avance
  }

  const cambiarMes = (texto: string) => {
    const nuevo = limpiar(texto, 2)
    setMes(nuevo)
    notificar(dia, nuevo, anio)
    if (nuevo.length === 2) refAnio.current?.focus()
  }

  const cambiarAnio = (texto: string) => {
    const nuevo = limpiar(texto, 4)
    setAnio(nuevo)
    notificar(dia, mes, nuevo)
  }

  // Backspace con el casillero ya vacío: salta al casillero anterior para seguir borrando
  const retroceder = (evento: React.KeyboardEvent<HTMLInputElement>, actual: string, anterior: React.RefObject<HTMLInputElement | null>) => {
    if (evento.key === 'Backspace' && actual === '') anterior.current?.focus()
  }

  const clase = `form-control text-center ${hayError ? 'is-invalid' : ''}`

  return (
    <div className="d-flex fecha-nacimiento">
      <input
        id={id}
        ref={refDia}
        className={`${clase} mr-1`}
        style={{ width: 70 }}
        inputMode="numeric"
        placeholder="DD"
        aria-label="Día de nacimiento"
        autoComplete="off"
        value={dia}
        onChange={(e) => cambiarDia(e.target.value)}
      />
      <input
        ref={refMes}
        className={`${clase} mr-1`}
        style={{ width: 70 }}
        inputMode="numeric"
        placeholder="MM"
        aria-label="Mes de nacimiento"
        autoComplete="off"
        value={mes}
        onChange={(e) => cambiarMes(e.target.value)}
        onKeyDown={(e) => retroceder(e, mes, refDia)}
      />
      <input
        ref={refAnio}
        className={clase}
        style={{ width: 90 }}
        inputMode="numeric"
        placeholder="AAAA"
        aria-label="Año de nacimiento"
        autoComplete="off"
        value={anio}
        onChange={(e) => cambiarAnio(e.target.value)}
        onKeyDown={(e) => retroceder(e, anio, refMes)}
      />
    </div>
  )
}
