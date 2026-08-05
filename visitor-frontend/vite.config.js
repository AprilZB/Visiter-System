import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 8097,
    host: '0.0.0.0',
    proxy: {
      '/api': {
        target: 'http://localhost:8096',
        changeOrigin: true
      }
    }
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  }
})
