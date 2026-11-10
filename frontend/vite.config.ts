import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // loadEnv lee las variables del archivo .env (y del entorno) sin necesitar los tipos de Node
  const env = loadEnv(mode, '.', '')

  return {
    plugins: [react()],
    server: {
      // puerto fijo: el backend tiene este origen como default de CORS (app.cors.origen-permitido)
      port: 5173,
      strictPort: true,
      /*
      Proxy de desarrollo: el front llama a "/api/..." y Vite lo reenvía al backend (puerto 8080).
      Para el navegador todo viene del mismo origen, así que en desarrollo ni siquiera se necesita CORS.
      Se puede cambiar el destino con la variable VITE_PROXY_TARGET.
    */
      proxy: {
        '/api': {
          target: env.VITE_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
  }
})
