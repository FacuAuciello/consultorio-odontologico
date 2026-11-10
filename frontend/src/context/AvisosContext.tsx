import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'

/*
 Avisos ("flash messages"): el cartel verde "Paciente guardado correctamente" o el rojo de error.
 En la versión con JSP se guardaban en la sesión del servidor y se leían en la siguiente página.
 Acá el contexto vive por encima de las rutas, así que el aviso sobrevive al cambio de pantalla (guardar -> volver a la lista).
 */

export type TipoAviso = 'exito' | 'error'

export interface Aviso {
  id: number
  tipo: TipoAviso
  contenido: ReactNode
}

interface AvisosContextoValor {
  avisos: Aviso[]
  mostrarAviso: (tipo: TipoAviso, contenido: ReactNode) => void
  cerrarAviso: (id: number) => void
}

// los de éxito desaparecen solos a los 3 segundos (igual que antes); los de error duran más para dar tiempo a leerlos
const DURACION_MS: Record<TipoAviso, number> = { exito: 3000, error: 7000 }

const AvisosContext = createContext<AvisosContextoValor | null>(null)

let siguienteId = 1

export function AvisosProvider({ children }: { children: ReactNode }) {
  const [avisos, setAvisos] = useState<Aviso[]>([])

  const cerrarAviso = useCallback((id: number) => {
    // forma funcional del setState: recibe el estado ACTUAL, así no se pierde un aviso si llegan dos juntos
    setAvisos((actuales) => actuales.filter((aviso) => aviso.id !== id))
  }, [])

  const mostrarAviso = useCallback(
    (tipo: TipoAviso, contenido: ReactNode) => {
      const id = siguienteId++
      setAvisos((actuales) => [...actuales, { id, tipo, contenido }])
      setTimeout(() => cerrarAviso(id), DURACION_MS[tipo])
    },
    [cerrarAviso],
  )

  const valor = useMemo(() => ({ avisos, mostrarAviso, cerrarAviso }), [avisos, mostrarAviso, cerrarAviso])

  return <AvisosContext.Provider value={valor}>{children}</AvisosContext.Provider>
}

export function useAvisos(): AvisosContextoValor {
  const contexto = useContext(AvisosContext)
  if (!contexto) throw new Error('useAvisos tiene que usarse dentro de <AvisosProvider>')
  return contexto
}
