import { useEffect, useState } from 'react'
import { Outlet } from 'react-router-dom'
import AvisosContenedor from '../ui/AvisosContenedor'
import Sidebar from './Sidebar'
import Topbar from './Topbar'

/*
 Estructura fija de todas las pantallas internas (menú lateral + barra superior + pie).
 <Outlet /> es el "hueco" donde React Router dibuja la pantalla de cada ruta: el layout se monta una sola vez
 y solo cambia el contenido del medio al navegar.
 */
export default function Layout() {
  // en celulares muy angostos el menú arranca oculto
  const [colapsado, setColapsado] = useState(() => window.innerWidth < 480)

  // SB Admin 2 también necesita la clase "sidebar-toggled" en el <body> (la usa para ajustar el pie de página)
  useEffect(() => {
    document.body.classList.toggle('sidebar-toggled', colapsado)
    return () => document.body.classList.remove('sidebar-toggled')
  }, [colapsado])

  const alternar = () => setColapsado((actual) => !actual)

  return (
    <div id="wrapper">
      <Sidebar colapsado={colapsado} alAlternar={alternar} />

      <div id="content-wrapper" className="d-flex flex-column">
        <div id="content">
          <Topbar alAlternarMenu={alternar} />

          <div className="container-fluid">
            <AvisosContenedor />
            <Outlet />
          </div>
        </div>

        <footer className="sticky-footer bg-white">
          <div className="container my-auto">
            <div className="copyright text-center my-auto">
              <span>Facundo Auciello &copy; {new Date().getFullYear()}</span>
            </div>
          </div>
        </footer>
      </div>
    </div>
  )
}
