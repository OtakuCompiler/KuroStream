import React from 'react'
import ReactDOM from 'react-dom/client'
import { HashRouter } from 'react-router-dom'
import App from './App'
import './styles/global.css'

// Service Worker registration for offline support
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(() => {})
  })
}

// Memory pressure handler
if ('storage' in navigator && 'estimate' in navigator.storage) {
  setInterval(async () => {
    const estimate = await navigator.storage.estimate()
    if (estimate.usage && estimate.quota && estimate.usage / estimate.quota > 0.85) {
      window.dispatchEvent(new CustomEvent('arctic:memory-pressure', { detail: { level: 'critical' } }))
    }
  }, 30000)
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <HashRouter>
      <App />
    </HashRouter>
  </React.StrictMode>
)
