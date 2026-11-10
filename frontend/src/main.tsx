import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'

// Estilos. El orden importa: primero la plantilla (SB Admin 2), después los ajustes propios, que pisan lo anterior
import '@fontsource/nunito/400.css'
import '@fontsource/nunito/600.css'
import '@fontsource/nunito/700.css'
import '@fontsource/nunito/800.css'
import '@fortawesome/fontawesome-free/css/all.min.css'
import './styles/sb-admin-2.min.css'
import './styles/app.css'

import App from './App'
import { AuthProvider } from './context/AuthContext'
import { AvisosProvider } from './context/AvisosContext'

/*
 Punto de entrada. Los "providers" envuelven a toda la aplicación para que cualquier pantalla pueda usar
 la sesión (useAuth) y los avisos (useAvisos) sin pasar datos de componente en componente.
 */
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <AvisosProvider>
          <App />
        </AvisosProvider>
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
)
