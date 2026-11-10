import { useEffect, useId, useMemo, useRef, useState } from 'react'
import type { PacienteResumen } from '../../types'
import { apellidoYNombre, normalizarTexto } from '../../utils/formato'

interface Props {
  id: string
  pacientes: PacienteResumen[]
  valor: number | null // idPaciente seleccionado
  alCambiar: (idPaciente: number | null) => void
  hayError?: boolean
}

// con miles de pacientes no se dibujan todos a la vez: se muestran los primeros y se pide seguir escribiendo
const MAXIMO_VISIBLES = 50

/*
 Lista desplegable con buscador (en la versión anterior era la librería Select2 con jQuery).
 Se escribió a mano para no depender de otra librería. Funciona así:
   - al hacer foco se abre la lista; al escribir se filtra por apellido, nombre o DNI (sin importar tildes ni mayúsculas)
   - teclado: flechas para moverse, Enter para elegir, Escape para cerrar
   - clic en la "x" para quitar la selección
 Sigue el patrón de accesibilidad "combobox" (los atributos aria-*) para que lo entiendan los lectores de pantalla.
 */
export default function PacienteSelect({ id, pacientes, valor, alCambiar, hayError = false }: Props) {
  const [abierto, setAbierto] = useState(false)
  const [texto, setTexto] = useState('')
  const [indiceActivo, setIndiceActivo] = useState(0)

  const idLista = useId()
  const refLista = useRef<HTMLUListElement>(null)

  const seleccionado = useMemo(() => pacientes.find((p) => p.idPaciente === valor) ?? null, [pacientes, valor])

  // filtrado: se juntan los tres datos buscables en un solo texto normalizado y se comprueba si contiene lo escrito
  const opciones = useMemo(() => {
    const buscado = normalizarTexto(texto.trim())
    return pacientes.filter((p) => normalizarTexto(`${p.apellido} ${p.nombre} ${p.dni}`).includes(buscado))
  }, [pacientes, texto])

  const visibles = opciones.slice(0, MAXIMO_VISIBLES)

  // mantiene a la vista la opción marcada con las flechas cuando la lista tiene scroll
  useEffect(() => {
    if (!abierto) return
    refLista.current?.children[indiceActivo]?.scrollIntoView?.({ block: 'nearest' })
  }, [indiceActivo, abierto])

  const elegir = (paciente: PacienteResumen) => {
    alCambiar(paciente.idPaciente)
    setAbierto(false)
    setTexto('')
  }

  const manejarTeclado = (evento: React.KeyboardEvent<HTMLInputElement>) => {
    switch (evento.key) {
      case 'ArrowDown':
        evento.preventDefault() // evita que el cursor salte al final del texto
        setAbierto(true)
        setIndiceActivo((i) => Math.min(i + 1, visibles.length - 1))
        break
      case 'ArrowUp':
        evento.preventDefault()
        setIndiceActivo((i) => Math.max(i - 1, 0))
        break
      case 'Enter':
        // si la lista está abierta, Enter elige la opción marcada en vez de enviar el formulario
        if (abierto && visibles[indiceActivo]) {
          evento.preventDefault()
          elegir(visibles[indiceActivo])
        }
        break
      case 'Escape':
        setAbierto(false)
        setTexto('')
        break
    }
  }

  // cerrada: se muestra el paciente elegido. Abierta: lo que el usuario está escribiendo para buscar
  const textoDelInput = abierto ? texto : seleccionado ? apellidoYNombre(seleccionado) : ''

  return (
    <div className="combo">
      <div className="input-group">
        <input
          id={id}
          className={`form-control ${hayError ? 'is-invalid' : ''}`}
          role="combobox"
          aria-expanded={abierto}
          aria-controls={idLista}
          aria-autocomplete="list"
          aria-activedescendant={abierto && visibles[indiceActivo] ? `${idLista}-${indiceActivo}` : undefined}
          placeholder="Buscar paciente por apellido, nombre o DNI..."
          autoComplete="off"
          value={textoDelInput}
          onFocus={() => {
            setAbierto(true)
            setTexto('')
            setIndiceActivo(0)
          }}
          // al perder el foco se cierra. Las opciones usan onMouseDown (que ocurre ANTES del blur) para alcanzar a registrar el clic
          onBlur={() => {
            setAbierto(false)
            setTexto('')
          }}
          onChange={(e) => {
            setTexto(e.target.value)
            setIndiceActivo(0)
            setAbierto(true)
          }}
          onKeyDown={manejarTeclado}
        />
        {seleccionado && (
          <div className="input-group-append">
            <button type="button" className="btn btn-outline-secondary" aria-label="Quitar paciente" onMouseDown={(e) => e.preventDefault()} onClick={() => alCambiar(null)}>
              <i className="fas fa-times"></i>
            </button>
          </div>
        )}
      </div>

      {abierto && (
        <ul id={idLista} ref={refLista} role="listbox" className="combo-lista list-unstyled shadow">
          {visibles.length === 0 && <li className="combo-vacio">No se encontraron pacientes</li>}

          {visibles.map((paciente, indice) => (
            <li
              key={paciente.idPaciente}
              id={`${idLista}-${indice}`}
              role="option"
              aria-selected={paciente.idPaciente === valor}
              className={`combo-opcion ${indice === indiceActivo ? 'activa' : ''}`}
              onMouseDown={(e) => {
                e.preventDefault() // evita que el input pierda el foco antes de registrar la selección
                elegir(paciente)
              }}
              onMouseEnter={() => setIndiceActivo(indice)}
            >
              <strong>{apellidoYNombre(paciente)}</strong>
              <span className="text-gray-600 small ml-2">DNI {paciente.dni}</span>
            </li>
          ))}

          {opciones.length > MAXIMO_VISIBLES && (
            <li className="combo-vacio">Mostrando {MAXIMO_VISIBLES} de {opciones.length}. Seguí escribiendo para acotar.</li>
          )}
        </ul>
      )}
    </div>
  )
}
