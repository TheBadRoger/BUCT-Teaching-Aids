import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      '@shared-images': fileURLToPath(new URL('../api/java/src/main/resources/static/images', import.meta.url)),
    }
  },
  test: { environment: 'node', clearMocks: true },
  server: {
    proxy: {
      // 分模块迁移期间，登录/绑定页面及其资源仍由现有 Java 服务提供。
      '^/(enter|register|personal-center)\\.html$': { target: 'http://localhost:80', changeOrigin: true },
      '/js/': { target: 'http://localhost:80', changeOrigin: true },
      '/css/': { target: 'http://localhost:80', changeOrigin: true },
      '/images/': { target: 'http://localhost:80', changeOrigin: true },
      '/teacher-ai-api': {
        target: 'http://localhost:3000',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/teacher-ai-api/, '/api')
      },
      '/api': {
        target: 'http://localhost:80',
        changeOrigin: true,
        secure: false
      }
    }
  }
})
