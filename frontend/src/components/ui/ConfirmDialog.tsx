import { useEffect, useRef, type ReactNode } from 'react'

interface Props {
  titulo: string
  children: ReactNode
  textoConfirmar: string
  // true mientras se está ejecutando la acción: deshabilita los botones para evitar un doble clic
  procesando?: boolean
  alConfirmar: () => void
  alCancelar: () => void
}

/*
 Ventana de confirmación (reemplaza al confirm() nativo del navegador, que es feo y no se puede personalizar).
 Usa las clases CSS de los modales de Bootstrap, pero se muestra / oculta con React en vez de con el JavaScript de Bootstrap:
 el componente padre decide si lo renderiza o no.
 */
export default function ConfirmDialog({ titulo, children, textoConfirmar, procesando = false, alConfirmar, alCancelar }: Props) {
  const botonCancelar = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    // el foco empieza en "Cancelar": si el usuario aprieta Enter sin mirar, no se borra nada por accidente
    botonCancelar.current?.focus()

    // "modal-open" bloquea el scroll de la página de fondo mientras el diálogo está abierto
    document.body.classList.add('modal-open')

    const alTeclear = (evento: KeyboardEvent) => {
      if (evento.key === 'Escape') alCancelar()
    }
    document.addEventListener('keydown', alTeclear)

    return () => {
      document.body.classList.remove('modal-open')
      document.removeEventListener('keydown', alTeclear)
    }
  }, [alCancelar])

  return (
    <>
      <div className="modal-backdrop fade show"></div>
      <div className="modal fade show d-block" role="dialog" aria-modal="true" aria-labelledby="titulo-dialogo" onClick={alCancelar}>
        {/* stopPropagation: un clic DENTRO del cuadro no tiene que llegar al fondo (que lo cierra) */}
        <div className="modal-dialog modal-dialog-centered" onClick={(e) => e.stopPropagation()}>
          <div className="modal-content">
            <div className="modal-header">
              <h5 className="modal-title" id="titulo-dialogo">
                {titulo}
              </h5>
            </div>
            <div className="modal-body">{children}</div>
            <div className="modal-footer">
              <button ref={botonCancelar} type="button" className="btn btn-secondary" onClick={alCancelar} disabled={procesando}>
                Cancelar
              </button>
              <button type="button" className="btn btn-danger" onClick={alConfirmar} disabled={procesando}>
                {procesando && <span className="spinner-border spinner-border-sm mr-2" aria-hidden="true"></span>}
                {textoConfirmar}
              </button>
            </div>
          </div>
        </div>
      </div>
    </>
  )
}
