import type { ReactNode } from 'react'

interface Props {
  icono: string
  titulo: string
  children?: ReactNode
}

// lo que se muestra cuando una lista no tiene resultados
export default function EstadoVacio({ icono, titulo, children }: Props) {
  return (
    <div className="estado-vacio text-center py-5">
      <i className={`fas ${icono} fa-3x text-gray-300 mb-3`}></i>
      <h5 className="text-gray-700">{titulo}</h5>
      <div className="text-gray-600 small">{children}</div>
    </div>
  )
}
