import type { WatchHistory } from '../types'

const TRAKT_API = 'https://api.trakt.tv'

export async function syncWithTrakt(clientId: string, accessToken?: string): Promise<{ history: WatchHistory[] } | null> {
  try {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'trakt-api-version': '2',
      'trakt-api-key': clientId,
    }
    if (accessToken) headers['Authorization'] = `Bearer ${accessToken}`

    const res = await fetch(`${TRAKT_API}/users/me/history`, { headers })
    if (!res.ok) return null
    const data = await res.json()

    const history: WatchHistory[] = data.map((item: any) => ({
      titleId: `trakt-${item.show?.ids?.trakt || item.movie?.ids?.trakt}`,
      episodeId: item.episode ? `trakt-ep-${item.episode.season}-${item.episode.number}` : undefined,
      progress: item.progress || 0,
      duration: item.show?.runtime || item.movie?.runtime || 0,
      completed: item.progress > 0.9,
      timestamp: new Date(item.watched_at).getTime(),
      watchCount: 1,
    }))

    return { history }
  } catch { return null }
}

export async function getTraktCalendar(clientId: string, accessToken?: string): Promise<any[]> {
  try {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'trakt-api-version': '2',
      'trakt-api-key': clientId,
    }
    if (accessToken) headers['Authorization'] = `Bearer ${accessToken}`

    const res = await fetch(`${TRAKT_API}/calendars/my/shows`, { headers })
    if (!res.ok) return []
    return await res.json()
  } catch { return [] }
}
