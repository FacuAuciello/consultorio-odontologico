import { useEffect, useState } from 'react'

/*
 Devuelve el valor recién DESPUÉS de que dejó de cambiar durante "demoraMs".
 Se usa en los buscadores: mientras el usuario escribe "gomez" no se hacen 5 llamadas a la API (una por letra),
 se hace una sola cuando hace una pausa.
 */
export function useDebounce<T>(valor: T, demoraMs = 300): T {
  const [valorDemorado, setValorDemorado] = useState(valor)

  useEffect(() => {
    const temporizador = setTimeout(() => setValorDemorado(valor), demoraMs)
    // cada vez que el valor cambia antes de cumplirse la demora, se cancela el temporizador anterior y arranca uno nuevo
    return () => clearTimeout(temporizador)
  }, [valor, demoraMs])

  return valorDemorado
}
