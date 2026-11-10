import type { ReactNode } from 'react'

interface Props {
  etiqueta: string
  // id del input: une la etiqueta con el campo (al hacer clic en el texto se enfoca el input)
  htmlFor: string
  obligatorio?: boolean
  error?: string
  children: ReactNode
}

// envoltorio de un campo de formulario: etiqueta + input + mensaje de error. Evita repetir este bloque en cada campo
export default function Campo({ etiqueta, htmlFor, obligatorio = false, error, children }: Props) {
  return (
    <div className="form-group">
      <label htmlFor={htmlFor}>
        {etiqueta} {obligatorio && <span className="text-danger">*</span>}
      </label>
      {children}
      {/* d-block: Bootstrap oculta .invalid-feedback salvo que el input hermano esté marcado; acá se controla desde React */}
      {error && <div className="invalid-feedback d-block">{error}</div>}
    </div>
  )
}
