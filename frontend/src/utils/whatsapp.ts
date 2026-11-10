import type { Turno } from '../types'
import { formatearFecha, formatearHora, parsearFechaHora } from './fechas'
import { NOMBRE_PROFESIONAL } from '../config/consultorio'

/*
 Deja el teléfono listo para wa.me: solo dígitos y con el código de país de Argentina (54).
   "223 555-0101"      -> "542235550101"
   "+54 223 555-0101"  -> "542235550101"
   "0223 555-0101"     -> "542235550101"  (se saca el 0 inicial del prefijo)
 */
export function normalizarTelefono(telefono: string): string {
  const digitos = telefono.replace(/\D/g, '').replace(/^0+/, '')
  return digitos.startsWith('54') ? digitos : `54${digitos}`
}

// link que abre WhatsApp con el recordatorio del turno ya escrito
export function armarLinkWhatsApp(turno: Turno): string {
  const inicio = parsearFechaHora(turno.fechaHora)

  const mensaje =
    `Hola ${turno.nombrePaciente} ${turno.apellidoPaciente}, le recordamos su turno en el consultorio del ` +
    `${NOMBRE_PROFESIONAL} el día ${formatearFecha(turno.fechaHora.slice(0, 10))} a las ${formatearHora(inicio)}hs. ` +
    `Ante cualquier consulta comuníquese con nosotros.`

  // encodeURIComponent escapa espacios, tildes y signos para que viajen bien dentro de la URL
  return `https://wa.me/${normalizarTelefono(turno.telefonoPaciente)}?text=${encodeURIComponent(mensaje)}`
}
