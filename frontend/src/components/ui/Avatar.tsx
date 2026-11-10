import { iniciales } from '../../utils/formato'

// colores de la paleta de SB Admin 2
const COLORES = ['#4e73df', '#1cc88a', '#36b9cc', '#f6c23e', '#e74a3b', '#6f42c1', '#fd7e14', '#e83e8c']

/*
 Círculo con las iniciales de la persona. El color se calcula a partir del nombre (suma de los códigos de sus letras):
 la misma persona siempre tiene el mismo color, pero personas distintas se distinguen de un vistazo
 */
function colorPara(texto: string): string {
  const suma = [...texto].reduce((acumulado, letra) => acumulado + letra.charCodeAt(0), 0)
  return COLORES[suma % COLORES.length]
}

export default function Avatar({ nombre, apellido, grande = false }: { nombre: string; apellido: string; grande?: boolean }) {
  return (
    <span className={`avatar ${grande ? 'avatar-grande' : ''}`} style={{ backgroundColor: colorPara(nombre + apellido) }} aria-hidden="true">
      {iniciales(nombre, apellido)}
    </span>
  )
}
