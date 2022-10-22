import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import * as path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      'src': path.resolve(__dirname, './src')
    }
  },
  server: {
    proxy: {
      '/graphql': {
        target: `http://127.0.0.1:4000`,
        changeOrigin: true,
      },
      '/subscriptions': {
        target: `http://127.0.0.1:4000`,
        changeOrigin: true,
        ws: true,
      }
    }
  }
})
