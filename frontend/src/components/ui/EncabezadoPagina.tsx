import type { ReactNode } from 'react'

interface Props {
  titulo: ReactNode
  // botones de acción alineados a la derecha
  children?: ReactNode
}

export default function EncabezadoPagina({ titulo, children }: Props) {
  return (
    <div className="d-sm-flex align-items-center justify-content-between mb-4">
      <h1 className="h3 mb-2 mb-sm-0 text-gray-800">{titulo}</h1>
      {children && <div className="d-flex flex-wrap gap-2 encabezado-acciones">{children}</div>}
    </div>
  )
}
