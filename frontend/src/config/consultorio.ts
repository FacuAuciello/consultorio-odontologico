/*
 Datos propios del consultorio. Es el UNICO lugar donde viven: el menú, el login y el mensaje de WhatsApp los leen de acá.
 Se pueden cambiar con variables VITE_ (ver .env.example) sin tocar el código.
 */
export const NOMBRE_CONSULTORIO: string = import.meta.env.VITE_NOMBRE_CONSULTORIO ?? 'Consultorio Odontológico'
export const NOMBRE_PROFESIONAL: string = import.meta.env.VITE_NOMBRE_PROFESIONAL ?? 'Dr. Demo (ficticio)'

// URL base de la API. Vacío = mismo origen (en desarrollo, el proxy de vite.config.ts reenvía /api al backend)
export const API_URL: string = import.meta.env.VITE_API_URL ?? ''
