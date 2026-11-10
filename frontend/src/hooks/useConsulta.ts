import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError } from '../api/cliente'

/*
 Hook para cargar datos de la API y manejar los tres estados de siempre: cargando, error y datos.
 Uso:
     const { datos, cargando, error, recargar } = useConsulta(() => pacientesApi.listar(busqueda), [busqueda])
 Vuelve a consultar automáticamente cada vez que cambia algo de la lista "deps" (igual que useEffect).
 */
export interface ResultadoConsulta<T> {
  datos: T | null
  cargando: boolean
  error: string | null
  recargar: () => void
}

export function useConsulta<T>(consulta: () => Promise<T>, deps: readonly unknown[]): ResultadoConsulta<T> {
  const [datos, setDatos] = useState<T | null>(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)
  // contador que cambia al pedir "recargar": como forma parte de las dependencias del efecto, fuerza una nueva consulta
  const [version, setVersion] = useState(0)

  // guardamos la función de consulta en una ref para usar siempre la última, sin que su identidad dispare el efecto
  const consultaRef = useRef(consulta)
  consultaRef.current = consulta

  useEffect(() => {
    // "cancelado" evita una carrera: si el usuario cambia el filtro dos veces rápido, la respuesta de la primera
    // consulta (que puede llegar DESPUÉS) no tiene que pisar a la de la segunda
    let cancelado = false

    setCargando(true)
    setError(null)

    consultaRef
      .current()
      .then((resultado) => {
        if (!cancelado) setDatos(resultado)
      })
      .catch((e: unknown) => {
        if (!cancelado) setError(e instanceof ApiError ? e.message : 'Ocurrió un error inesperado')
      })
      .finally(() => {
        if (!cancelado) setCargando(false)
      })

    // limpieza: al cambiar las dependencias o desmontar el componente se marca como cancelada la consulta en curso
    return () => {
      cancelado = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, version])

  const recargar = useCallback(() => setVersion((v) => v + 1), [])

  return { datos, cargando, error, recargar }
}
