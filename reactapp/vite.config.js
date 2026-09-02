import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// This proxy means the frontend can call "/api/..." and Vite will
// forward it to the real Spring Boot backend during development,
// so you never hit CORS issues and never hardcode the backend port
// in every fetch call.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
})
