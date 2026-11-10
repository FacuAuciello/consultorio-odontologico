import { desformatearNumero, formatearConPuntos } from '../../utils/formato'

interface Props {
  id: string
  valor: number | null
  alCambiar: (valor: number | null) => void
}

// la columna del backend admite hasta 10 dígitos enteros (DECIMAL(12,2))
const MAXIMO = 9_999_999_999

/*
 Campo de dinero en pesos enteros. Mientras se escribe se muestra con punto de miles ("15.000"),
 pero hacia afuera entrega un NÚMERO (15000), que es lo que espera la API.
 En la versión con JSP esto se resolvía con dos inputs (uno visible con puntos y otro oculto sin puntos);
 en React alcanza con guardar el número y calcular el texto en cada render.
 */
export default function MontoInput({ id, valor, alCambiar }: Props) {
  const manejarCambio = (texto: string) => {
    const numero = desformatearNumero(texto) // descarta todo lo que no sea dígito
    if (numero !== null && numero > MAXIMO) return // ignora el dígito que haría pasar el máximo
    alCambiar(numero)
  }

  return (
    <div className="input-group">
      <div className="input-group-prepend">
        <span className="input-group-text">$</span>
      </div>
      <input
        id={id}
        className="form-control"
        inputMode="numeric"
        placeholder="0"
        autoComplete="off"
        value={valor === null ? '' : formatearConPuntos(valor)}
        onChange={(e) => manejarCambio(e.target.value)}
      />
    </div>
  )
}
