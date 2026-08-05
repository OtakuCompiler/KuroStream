export interface Notification {
  id: string
  title: string
  message: string
  type: 'info' | 'success' | 'warning' | 'error'
  timestamp: number
  read: boolean
  action?: { label: string; url: string }
}

const NOTIFICATIONS_KEY = 'arctic-notifications'

export function getNotifications(): Notification[] {
  try {
    return JSON.parse(localStorage.getItem(NOTIFICATIONS_KEY) || '[]')
  } catch { return [] }
}

export function addNotification(notification: Omit<Notification, 'id' | 'timestamp' | 'read'>): void {
  const all = getNotifications()
  all.unshift({
    ...notification,
    id: Math.random().toString(36).substring(2),
    timestamp: Date.now(),
    read: false,
  })
  localStorage.setItem(NOTIFICATIONS_KEY, JSON.stringify(all.slice(0, 50)))
}

export function markNotificationRead(id: string): void {
  const all = getNotifications()
  const updated = all.map(n => n.id === id ? { ...n, read: true } : n)
  localStorage.setItem(NOTIFICATIONS_KEY, JSON.stringify(updated))
}

export function clearNotifications(): void {
  localStorage.setItem(NOTIFICATIONS_KEY, '[]')
}

export function checkNewEpisodes(library: string[]): void {
  // Check for new episodes of tracked shows
  // This would poll APIs in a real implementation
}
