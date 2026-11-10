export default function Cargando({ texto = 'Cargando...' }: { texto?: string }) {
  return (
    <div className="text-center text-gray-600 py-5" role="status">
      <div className="spinner-border text-primary mb-3" aria-hidden="true"></div>
      <div className="small">{texto}</div>
    </div>
  )
}
