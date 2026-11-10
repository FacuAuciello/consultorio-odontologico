import { Link } from 'react-router-dom'
import EstadoVacio from '../components/ui/EstadoVacio'

export default function NoEncontrada() {
  return (
    <div className="card shadow mb-4">
      <div className="card-body">
        <EstadoVacio icono="fa-map-signs" titulo="Esta página no existe">
          <Link to="/" className="btn btn-primary btn-sm mt-2">
            Volver a la agenda
          </Link>
        </EstadoVacio>
      </div>
    </div>
  )
}
