interface Props {
  pagina: number // base 0: la primera página es 0
  tamanio: number
  total: number // cantidad total de registros (no de páginas)
  alCambiar: (pagina: number) => void
}

/*
 Números de página a mostrar. Con muchas páginas no se muestran todas: se muestra la primera, la última y
 las cercanas a la actual, con "…" en los saltos. Ejemplo estando en la página 7 de 20:  1 … 6 7 8 … 20
 Devuelve números (base 0) y null para cada "…".
 */
export function ventanaDePaginas(actual: number, totalPaginas: number): (number | null)[] {
  const resultado: (number | null)[] = []
  let anterior = -1

  for (let i = 0; i < totalPaginas; i++) {
    const esExtremo = i === 0 || i === totalPaginas - 1
    const esCercana = Math.abs(i - actual) <= 1

    if (esExtremo || esCercana) {
      // si entre la página anterior mostrada y esta hay un hueco, se marca con "…"
      if (anterior !== -1 && i - anterior > 1) resultado.push(null)
      resultado.push(i)
      anterior = i
    }
  }
  return resultado
}

export default function Paginacion({ pagina, tamanio, total, alCambiar }: Props) {
  const totalPaginas = Math.max(1, Math.ceil(total / tamanio))
  const desde = total === 0 ? 0 : pagina * tamanio + 1
  const hasta = Math.min(total, (pagina + 1) * tamanio)

  return (
    <div className="d-flex flex-column flex-md-row align-items-md-center justify-content-between mt-3">
      <div className="small text-gray-600 mb-2 mb-md-0">
        Mostrando {desde} a {hasta} de {total} registros
      </div>

      {totalPaginas > 1 && (
        <nav aria-label="Paginación">
          <ul className="pagination pagination-sm mb-0">
            <li className={`page-item ${pagina === 0 ? 'disabled' : ''}`}>
              <button className="page-link" onClick={() => alCambiar(pagina - 1)} disabled={pagina === 0}>
                Anterior
              </button>
            </li>

            {ventanaDePaginas(pagina, totalPaginas).map((numero, indice) =>
              numero === null ? (
                <li key={`salto-${indice}`} className="page-item disabled">
                  <span className="page-link">…</span>
                </li>
              ) : (
                <li key={numero} className={`page-item ${numero === pagina ? 'active' : ''}`}>
                  <button className="page-link" onClick={() => alCambiar(numero)}>
                    {numero + 1}
                  </button>
                </li>
              ),
            )}

            <li className={`page-item ${pagina >= totalPaginas - 1 ? 'disabled' : ''}`}>
              <button className="page-link" onClick={() => alCambiar(pagina + 1)} disabled={pagina >= totalPaginas - 1}>
                Siguiente
              </button>
            </li>
          </ul>
        </nav>
      )}
    </div>
  )
}
