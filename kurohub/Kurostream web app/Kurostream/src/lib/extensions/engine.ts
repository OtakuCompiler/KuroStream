import type { Title, Source, ExtensionManifest } from '../types'

export interface StremioManifest {
  id: string
  name: string
  version: string
  description: string
  types: string[]
  catalogs: { type: string; id: string; name: string }[]
  resources: string[]
}

export interface StremioStream {
  name?: string
  title?: string
  url?: string
  externalUrl?: string
  ytId?: string
  infoHash?: string
  fileIdx?: number
  behaviorHints?: { proxyHeaders?: any; notWebReady?: boolean }
}

export async function fetchStremioManifest(url: string): Promise<StremioManifest | null> {
  try {
    const res = await fetch(`${url}/manifest.json`)
    if (!res.ok) return null
    return await res.json()
  } catch { return null }
}

export async function searchStremioCatalog(url: string, type: string, id: string, query?: string): Promise<Title[]> {
  try {
    const q = query ? `?search=${encodeURIComponent(query)}` : ''
    const res = await fetch(`${url}/catalog/${type}/${id}/catalog.json${q}`)
    if (!res.ok) return []
    const data = await res.json()
    return (data.metas || []).map((m: any) => ({
      id: `stremio-${m.id}`,
      title: m.name,
      type: m.type === 'movie' ? 'movie' : 'series',
      year: m.releaseInfo ? parseInt(m.releaseInfo) : undefined,
      poster: m.poster,
      banner: m.background,
      overview: m.description,
      genres: m.genres || [],
      tags: [],
      cast: [],
      studios: [],
      directors: [],
      status: 'completed',
      sources: ['Stremio'],
      ids: {},
    }))
  } catch { return [] }
}

export async function getStremioStreams(url: string, type: string, id: string): Promise<Source[]> {
  try {
    const res = await fetch(`${url}/stream/${type}/${id}.json`)
    if (!res.ok) return []
    const data = await res.json()
    return (data.streams || []).map((s: StremioStream, i: number) => {
      let sourceUrl = s.url || s.externalUrl || ''
      if (s.ytId) sourceUrl = `https://www.youtube.com/watch?v=${s.ytId}`
      let quality: Source['quality'] = 'unknown'
      const title = s.title || s.name || ''
      if (/\b(2160p|4K|UHD)\b/i.test(title)) quality = '4K'
      else if (/\b1080p\b/i.test(title)) quality = '1080p'
      else if (/\b720p\b/i.test(title)) quality = '720p'
      return {
        id: `stremio-${i}`,
        url: sourceUrl,
        quality,
        provider: 'Stremio',
        type: s.infoHash ? 'torrent' : sourceUrl.includes('.m3u8') ? 'hls' : 'direct',
        audioLang: ['en'],
        subtitleLang: [],
        score: 70,
      }
    })
  } catch { return [] }
}

export async function loadCloudStreamExtension(ext: any): Promise<{ search: (q: string) => Promise<Title[]>; load: (url: string) => Promise<Source[]> }> {
  return {
    search: async (q: string) => {
      try {
        const res = await fetch(`${ext.url}/search?q=${encodeURIComponent(q)}`)
        const data = await res.json()
        return (data.results || []).map((m: any) => ({
          id: `cs-${m.id}`, title: m.title, type: ext.type,
          poster: m.poster, overview: m.description, genres: m.genres || [],
          tags: [], cast: [], studios: [], directors: [], status: 'completed',
          sources: ['CloudStream'], ids: {},
        }))
      } catch { return [] }
    },
    load: async (url: string) => {
      try {
        const res = await fetch(`${ext.url}/load?url=${encodeURIComponent(url)}`)
        const data = await res.json()
        return (data.sources || []).map((s: any, i: number) => ({
          id: `cs-src-${i}`, url: s.url, quality: s.quality || 'unknown',
          provider: 'CloudStream', type: s.url.includes('.m3u8') ? 'hls' : 'direct',
          audioLang: [ext.lang], subtitleLang: [], score: 60,
        }))
      } catch { return [] }
    }
  }
}

export async function kodiRPC(endpoint: string, method: string, params?: any): Promise<any> {
  const res = await fetch(endpoint, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ jsonrpc: '2.0', id: 1, method, params }),
  })
  const data = await res.json()
  return data.result
}

export async function searchKodi(endpoint: string, query: string): Promise<Title[]> {
  try {
    const result = await kodiRPC(endpoint, 'VideoLibrary.GetMovies', {
      filter: { field: 'title', operator: 'contains', value: query },
      properties: ['title', 'plot', 'year', 'genre', 'thumbnail', 'fanart', 'rating', 'runtime'],
    })
    const movies = (result?.movies || []).map((m: any) => ({
      id: `kodi-${m.movieid}`, title: m.title, type: 'movie' as const,
      year: m.year, score: m.rating,
      poster: m.thumbnail ? `http://${endpoint.split('/')[2]}/image/${encodeURIComponent(m.thumbnail)}` : undefined,
      banner: m.fanart ? `http://${endpoint.split('/')[2]}/image/${encodeURIComponent(m.fanart)}` : undefined,
      overview: m.plot, genres: m.genre || [], tags: [], cast: [], studios: [], directors: [],
      duration: m.runtime, status: 'completed', sources: ['Kodi'], ids: {},
    }))
    const tvResult = await kodiRPC(endpoint, 'VideoLibrary.GetTVShows', {
      filter: { field: 'title', operator: 'contains', value: query },
      properties: ['title', 'plot', 'year', 'genre', 'thumbnail', 'fanart', 'rating'],
    })
    const shows = (tvResult?.tvshows || []).map((m: any) => ({
      id: `kodi-tv-${m.tvshowid}`, title: m.title, type: 'series' as const,
      year: m.year, score: m.rating,
      poster: m.thumbnail ? `http://${endpoint.split('/')[2]}/image/${encodeURIComponent(m.thumbnail)}` : undefined,
      banner: m.fanart ? `http://${endpoint.split('/')[2]}/image/${encodeURIComponent(m.fanart)}` : undefined,
      overview: m.plot, genres: m.genre || [], tags: [], cast: [], studios: [], directors: [],
      status: 'completed', sources: ['Kodi'], ids: {},
    }))
    return [...movies, ...shows]
  } catch { return [] }
}

export async function getKodiSources(endpoint: string, type: string, id: string): Promise<Source[]> {
  try {
    const realId = parseInt(id.split('-').pop() || '0')
    if (type === 'movie') {
      const result = await kodiRPC(endpoint, 'VideoLibrary.GetMovieDetails', { movieid: realId, properties: ['file', 'streamdetails'] })
      const file = result?.moviedetails?.file
      if (!file) return []
      return [{ id: `kodi-${realId}`, url: file, quality: 'unknown', provider: 'Kodi', type: 'direct', audioLang: ['en'], subtitleLang: [], score: 80 }]
    } else {
      const result = await kodiRPC(endpoint, 'VideoLibrary.GetEpisodeDetails', { episodeid: realId, properties: ['file', 'streamdetails'] })
      const file = result?.episodedetails?.file
      if (!file) return []
      return [{ id: `kodi-${realId}`, url: file, quality: 'unknown', provider: 'Kodi', type: 'direct', audioLang: ['en'], subtitleLang: [], score: 80 }]
    }
  } catch { return [] }
}

export async function fetchNuvioManifest(url: string): Promise<any | null> {
  try {
    const res = await fetch(`${url}/nuvio/manifest.json`)
    if (!res.ok) return null
    return await res.json()
  } catch { return null }
}

export async function searchNuvio(url: string, query: string): Promise<Title[]> {
  try {
    const res = await fetch(`${url}/nuvio/search?q=${encodeURIComponent(query)}`)
    const data = await res.json()
    return (data.results || []).map((m: any) => ({
      id: `nuvio-${m.id}`, title: m.title, type: m.type === 'movie' ? 'movie' : 'series',
      year: m.year, poster: m.poster, banner: m.banner, overview: m.description,
      genres: m.genres || [], tags: m.tags || [], cast: m.cast || [], studios: m.studios || [],
      directors: m.directors || [], status: m.status || 'completed', sources: ['Nuvio'], ids: m.ids || {},
    }))
  } catch { return [] }
}

export async function getNuvioStreams(url: string, id: string): Promise<Source[]> {
  try {
    const res = await fetch(`${url}/nuvio/stream/${id}`)
    const data = await res.json()
    return (data.streams || []).map((s: any, i: number) => ({
      id: `nuvio-${i}`, url: s.url, quality: s.quality || 'unknown', provider: 'Nuvio',
      type: s.url.includes('.m3u8') ? 'hls' : s.infoHash ? 'torrent' : 'direct',
      size: s.size, seeds: s.seeds, peers: s.peers, audioLang: s.audio || ['en'],
      subtitleLang: s.subtitles || [], codec: s.codec, hdr: s.hdr, score: s.score || 70,
    }))
  } catch { return [] }
}

export async function loadExtension(manifest: ExtensionManifest): Promise<{ search: (q: string) => Promise<Title[]>; getStreams: (id: string) => Promise<Source[]> } | null> {
  if (manifest.type === 'provider') {
    if (manifest.entry.includes('stremio')) {
      return {
        search: (q: string) => searchStremioCatalog(manifest.entry, 'movie', 'catalog', q),
        getStreams: (id: string) => getStremioStreams(manifest.entry, 'movie', id.replace('stremio-', '')),
      }
    }
    if (manifest.entry.includes('nuvio')) {
      return {
        search: (q: string) => searchNuvio(manifest.entry, q),
        getStreams: (id: string) => getNuvioStreams(manifest.entry, id.replace('nuvio-', '')),
      }
    }
    if (manifest.entry.includes('kodi')) {
      return {
        search: (q: string) => searchKodi(manifest.entry, q),
        getStreams: (id: string) => getKodiSources(manifest.entry, 'movie', id),
      }
    }
    if (manifest.entry.includes('cloudstream')) {
      const ext = await loadCloudStreamExtension({ name: manifest.name, url: manifest.entry, lang: 'en', type: 'movie' })
      return { search: ext.search, getStreams: ext.load }
    }
  }
  return null
}

export const BUILT_IN_EXTENSIONS: ExtensionManifest[] = [
  { id: 'stremio-cinemeta', name: 'Stremio Cinemeta', version: '1.0.0', author: 'Stremio', description: 'Official Stremio metadata addon', type: 'provider', entry: 'https://v3-cinemeta.strem.io', permissions: ['network'], installed: false, enabled: false },
  { id: 'stremio-tmdb', name: 'Stremio TMDB', version: '1.0.0', author: 'Stremio', description: 'TMDB catalog for Stremio', type: 'provider', entry: 'https://v3-tmdb.strem.io', permissions: ['network'], installed: false, enabled: false },
  { id: 'stremio-fanart', name: 'Stremio FanArt', version: '1.0.0', author: 'Stremio', description: 'FanArt.tv artwork', type: 'metadata', entry: 'https://v3-fanart.strem.io', permissions: ['network'], installed: false, enabled: false },
  { id: 'stremio-opensubtitles', name: 'Stremio OpenSubtitles', version: '1.0.0', author: 'Stremio', description: 'Subtitle provider', type: 'subtitle', entry: 'https://v3-opensubtitles.strem.io', permissions: ['network'], installed: false, enabled: false },
  { id: 'stremio-rotten', name: 'Stremio Rotten Tomatoes', version: '1.0.0', author: 'Stremio', description: 'Ratings from Rotten Tomatoes', type: 'metadata', entry: 'https://v3-rotten.strem.io', permissions: ['network'], installed: false, enabled: false },
  { id: 'stremio-imdb', name: 'Stremio IMDB', version: '1.0.0', author: 'Stremio', description: 'IMDB catalog', type: 'provider', entry: 'https://v3-imdb.strem.io', permissions: ['network'], installed: false, enabled: false },
  { id: 'stremio-youtube', name: 'Stremio YouTube', version: '1.0.0', author: 'Stremio', description: 'YouTube trailers', type: 'provider', entry: 'https://v3-youtube.strem.io', permissions: ['network'], installed: false, enabled: false },
  { id: 'stremio-channels', name: 'Stremio Channels', version: '1.0.0', author: 'Stremio', description: 'Live TV channels', type: 'provider', entry: 'https://v3-channels.strem.io', permissions: ['network'], installed: false, enabled: false },
  { id: 'nuvio-bridge', name: 'Nuvio Bridge', version: '1.0.0', author: 'Arctic', description: 'Nuvio streaming protocol', type: 'provider', entry: 'https://nuvio.example.com', permissions: ['network'], installed: false, enabled: false },
  { id: 'kodi-jsonrpc', name: 'Kodi JSON-RPC', version: '1.0.0', author: 'Arctic', description: 'Kodi media center integration', type: 'provider', entry: 'http://localhost:8080/jsonrpc', permissions: ['network'], installed: false, enabled: false },
  { id: 'cloudstream-proxy', name: 'CloudStream Proxy', version: '1.0.0', author: 'Arctic', description: 'CloudStream extension bridge', type: 'provider', entry: 'https://cs-proxy.arctic.dev', permissions: ['network'], installed: false, enabled: false },
  { id: 'opensubtitles', name: 'OpenSubtitles', version: '1.0.0', author: 'Arctic', description: 'Subtitle provider', type: 'subtitle', entry: 'https://api.opensubtitles.com', permissions: ['network'], installed: true, enabled: true },
  { id: 'subdl', name: 'SubDL', version: '1.0.0', author: 'Arctic', description: 'Subtitle provider', type: 'subtitle', entry: 'https://api.subdl.com', permissions: ['network'], installed: true, enabled: true },
  { id: 'addic7ed', name: 'Addic7ed', version: '1.0.0', author: 'Arctic', description: 'Subtitle provider', type: 'subtitle', entry: 'https://www.addic7ed.com', permissions: ['network'], installed: true, enabled: true },
  { id: 'podnapisi', name: 'Podnapisi', version: '1.0.0', author: 'Arctic', description: 'Subtitle provider', type: 'subtitle', entry: 'https://www.podnapisi.net', permissions: ['network'], installed: true, enabled: true },
  { id: 'aniskip', name: 'AniSkip', version: '1.0.0', author: 'Arctic', description: 'Anime intro/outro skip data', type: 'metadata', entry: 'https://api.aniskip.com', permissions: ['network'], installed: true, enabled: true },
  { id: 'trakt', name: 'Trakt.tv', version: '1.0.0', author: 'Arctic', description: 'Watch history sync', type: 'metadata', entry: 'https://api.trakt.tv', permissions: ['network'], installed: false, enabled: false },
  { id: 'fanart-tv', name: 'FanArt.tv', version: '1.0.0', author: 'Arctic', description: 'High quality artwork', type: 'metadata', entry: 'https://webservice.fanart.tv', permissions: ['network'], installed: false, enabled: false },
  { id: 'tmdb-images', name: 'TMDB Images', version: '1.0.0', author: 'Arctic', description: 'TMDB image provider', type: 'metadata', entry: 'https://api.themoviedb.org', permissions: ['network'], installed: true, enabled: true },
  { id: 'arctic-dark', name: 'Arctic Dark', version: '1.0.0', author: 'Arctic', description: 'Default dark theme', type: 'theme', entry: 'builtin:arctic-dark', permissions: [], installed: true, enabled: true },
  { id: 'arctic-midnight', name: 'Arctic Midnight', version: '1.0.0', author: 'Arctic', description: 'Deeper dark theme', type: 'theme', entry: 'builtin:arctic-midnight', permissions: [], installed: true, enabled: false },
  { id: 'arctic-ocean', name: 'Arctic Ocean', version: '1.0.0', author: 'Arctic', description: 'Ocean blue theme', type: 'theme', entry: 'builtin:arctic-ocean', permissions: [], installed: false, enabled: false },
  { id: 'weather-widget', name: 'Weather Widget', version: '1.0.0', author: 'Arctic', description: 'Display local weather', type: 'widget', entry: 'builtin:weather', permissions: ['location'], installed: false, enabled: false },
  { id: 'trakt-widget', name: 'Trakt Calendar', version: '1.0.0', author: 'Arctic', description: 'Upcoming episodes from Trakt', type: 'widget', entry: 'builtin:trakt-calendar', permissions: ['network'], installed: false, enabled: false },
]
