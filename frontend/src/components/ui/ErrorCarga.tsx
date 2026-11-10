interface Props {
  mensaje: string
  alReintentar?: () => void
}

// cartel de error cuando falla la carga de una pantalla, con botón para reintentar
export default function ErrorCarga({ mensaje, alReintentar }: Props) {
  return (
    <div className="alert alert-danger d-flex align-items-center justify-content-between" role="alert">
      <span>
        <i className="fas fa-exclamation-triangle mr-2"></i>
        {mensaje}
      </span>
      {alReintentar && (
        <button className="btn btn-sm btn-outline-danger ml-3" onClick={alReintentar}>
          Reintentar
        </button>
      )}
    </div>
  )
}
