import { useAvisos } from '../../context/AvisosContext'

// dibuja los avisos activos (cartel verde de éxito / rojo de error) con las clases de alerta de Bootstrap
export default function AvisosContenedor() {
  const { avisos, cerrarAviso } = useAvisos()

  return (
    <div aria-live="polite">
      {avisos.map((aviso) => (
        <div key={aviso.id} className={`alert ${aviso.tipo === 'exito' ? 'alert-success' : 'alert-danger'} alert-dismissible`} role="alert">
          <i className={`fas ${aviso.tipo === 'exito' ? 'fa-check-circle' : 'fa-exclamation-triangle'} mr-1`}></i> {aviso.contenido}
          <button type="button" className="close" onClick={() => cerrarAviso(aviso.id)} aria-label="Cerrar">
            <span aria-hidden="true">&times;</span>
          </button>
        </div>
      ))}
    </div>
  )
}
