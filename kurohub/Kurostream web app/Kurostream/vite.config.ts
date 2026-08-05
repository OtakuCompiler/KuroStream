import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { VitePWA } from 'vite-plugin-pwa'
import path from 'path'

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
    VitePWA({
      registerType: 'autoUpdate',
      workbox: {
        globPatterns: ['**/*.{js,css,html,ico,png,svg,woff2}'],
        maximumFileSizeToCacheInBytes: 50 * 1024 * 1024,
        runtimeCaching: [
          {
            urlPattern: /^https:\/\/.*/i,
            handler: 'CacheFirst',
            options: { cacheName: 'arctic-api-cache', expiration: { maxEntries: 500, maxAgeSeconds: 86400 } }
          }
        ]
      },
      manifest: {
        name: 'Arctic AIO Stream',
        short_name: 'Arctic AIO',
        description: 'Universal Streaming OS',
        theme_color: '#0a0a0f',
        background_color: '#0a0a0f',
        display: 'standalone',
        orientation: 'any',
        scope: '/',
        start_url: '/',
        icons: [
          { src: '/arctic-icon-192.png', sizes: '192x192', type: 'image/png' },
          { src: '/arctic-icon-512.png', sizes: '512x512', type: 'image/png' }
        ]
      }
    })
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      '@components': path.resolve(__dirname, './src/components'),
      '@lib': path.resolve(__dirname, './src/lib'),
      '@hooks': path.resolve(__dirname, './src/hooks'),
    }
  },
  build: {
    target: 'esnext',
    minify: 'terser',
    terserOptions: {
      compress: { drop_console: true, drop_debugger: true, passes: 3 }
    },
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom', 'react-router-dom'],
          player: ['hls.js', 'plyr', 'screenfull'],
          p2p: ['webtorrent'],
          ui: ['framer-motion', 'lucide-react'],
        }
      }
    },
    chunkSizeWarningLimit: 2000
  },
  server: {
    port: 3000,
    host: true
  }
})
