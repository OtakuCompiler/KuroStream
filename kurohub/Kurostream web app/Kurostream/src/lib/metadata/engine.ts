import type { Title, Episode } from '../types'

interface MetadataProvider {
  name: string
  priority: number
  weight: number
  latency: number
  search(query: string, type?: string): Promise<Title[]>
  getById(id: string): Promise<Title | null>
  getEpisodes(titleId: string, season?: number): Promise<Episode[]>
}

function mapAniListMedia(m: any): Title {
  return {
    id: `anilist-${m.id}`,
    title: m.title?.romaji || m.title?.english || 'Unknown',
    english: m.title?.english,
    japanese: m.title?.native,
    type: m.format === 'MOVIE' ? 'movie' : 'anime',
    year: m.seasonYear,
    score: m.averageScore ? m.averageScore / 10 : undefined,
    poster: m.coverImage?.extraLarge || m.coverImage?.large,
    banner: m.bannerImage,
    backdrop: m.bannerImage,
    overview: m.description?.replace(/<[^>]*>/g, ''),
    genres: m.genres || [],
    tags: (m.tags || []).map((t: any) => t.name),
    cast: (m.characters?.nodes || []).map((c: any) => c.name?.full).filter(Boolean),
    studios: (m.studios?.nodes || []).map((s: any) => s.name),
    directors: [],
    duration: m.duration,
    episodeCount: m.episodes,
    status: m.status === 'RELEASING' ? 'airing' : m.status === 'FINISHED' ? 'completed' : 'upcoming',
    sources: ['AniList'],
    ids: { anilist: m.id, mal: m.idMal },
    recommendations: (m.recommendations?.nodes || []).map((r: any) => `anilist-${r.mediaRecommendation?.id}`).filter(Boolean),
    related: (m.relations?.nodes || []).map((r: any) => `anilist-${r.id}`).filter(Boolean),
  }
}

function mapTMDBMedia(m: any, type: 'movie' | 'series', detail = false): Title {
  const base: Title = {
    id: `tmdb-${m.id}`,
    title: m.title || m.name,
    type,
    year: m.release_date ? parseInt(m.release_date) : m.first_air_date ? parseInt(m.first_air_date) : undefined,
    score: m.vote_average,
    poster: m.poster_path ? `https://image.tmdb.org/t/p/w500${m.poster_path}` : undefined,
    banner: m.backdrop_path ? `https://image.tmdb.org/t/p/original${m.backdrop_path}` : undefined,
    backdrop: m.backdrop_path ? `https://image.tmdb.org/t/p/original${m.backdrop_path}` : undefined,
    overview: m.overview,
    genres: (m.genres || []).map((g: any) => g.name),
    tags: [],
    cast: detail ? (m.credits?.cast || []).slice(0, 10).map((c: any) => c.name) : [],
    studios: detail ? (m.production_companies || []).map((p: any) => p.name) : [],
    directors: detail ? (m.credits?.crew || []).filter((c: any) => c.job === 'Director').map((c: any) => c.name) : [],
    duration: m.runtime,
    seasonCount: m.number_of_seasons,
    episodeCount: m.number_of_episodes,
    status: m.status === 'Released' || m.status === 'Ended' ? 'completed' : m.status === 'In Production' ? 'upcoming' : 'airing',
    sources: ['TMDB'],
    ids: { tmdb: m.id },
  }
  if (detail) {
    base.recommendations = (m.recommendations?.results || m.similar?.results || []).slice(0, 10).map((r: any) => `tmdb-${r.id}`)
  }
  return base
}

function mapKitsuMedia(m: any): Title {
  const attr = m.attributes || {}
  return {
    id: `kitsu-${m.id}`,
    title: attr.canonicalTitle || attr.titles?.en || attr.titles?.en_jp || 'Unknown',
    english: attr.titles?.en,
    japanese: attr.titles?.ja_jp,
    type: attr.showType === 'movie' ? 'movie' : 'anime',
    year: attr.startDate ? parseInt(attr.startDate) : undefined,
    score: attr.averageRating ? parseFloat(attr.averageRating) / 10 : undefined,
    poster: attr.posterImage?.original || attr.posterImage?.large,
    banner: attr.coverImage?.original || attr.coverImage?.large,
    overview: attr.synopsis,
    genres: [],
    tags: [],
    cast: [],
    studios: attr.studio ? [attr.studio] : [],
    directors: [],
    duration: attr.episodeLength,
    episodeCount: attr.episodeCount,
    status: attr.status === 'current' ? 'airing' : attr.status === 'finished' ? 'completed' : 'upcoming',
    sources: ['Kitsu'],
    ids: { kitsu: m.id },
  }
}

function mapTraktMedia(m: any): Title {
  return {
    id: `trakt-${m.ids?.trakt || m.id}`,
    title: m.title,
    type: m.type === 'movie' ? 'movie' : 'series',
    year: m.year,
    score: m.rating,
    poster: undefined,
    overview: m.overview,
    genres: m.genres || [],
    tags: [],
    cast: [],
    studios: [],
    directors: [],
    status: 'completed',
    sources: ['Trakt'],
    ids: { trakt: m.ids?.trakt, imdb: m.ids?.imdb, tmdb: m.ids?.tmdb },
  }
}

function mapOMDBMedia(m: any): Title {
  return {
    id: `omdb-${m.imdbID}`,
    title: m.Title,
    type: m.Type === 'movie' ? 'movie' : 'series',
    year: m.Year ? parseInt(m.Year) : undefined,
    score: m.imdbRating ? parseFloat(m.imdbRating) : undefined,
    poster: m.Poster !== 'N/A' ? m.Poster : undefined,
    overview: m.Plot !== 'N/A' ? m.Plot : undefined,
    genres: m.Genre ? m.Genre.split(', ').map((g: string) => g.trim()) : [],
    tags: [],
    cast: m.Actors ? m.Actors.split(', ').map((a: string) => a.trim()) : [],
    studios: m.Production ? m.Production.split(', ').map((s: string) => s.trim()) : [],
    directors: m.Director ? m.Director.split(', ').map((d: string) => d.trim()) : [],
    duration: m.Runtime ? parseInt(m.Runtime) : undefined,
    episodeCount: m.totalSeasons ? parseInt(m.totalSeasons) : undefined,
    status: 'completed',
    sources: ['OMDB'],
    ids: { imdb: m.imdbID },
  }
}

const anilistProvider: MetadataProvider = {
  name: 'AniList',
  priority: 1, weight: 1.0, latency: 0,
  async search(query: string, type?: string): Promise<Title[]> {
    const gql = `query { Page(perPage: 20) { media(search: "${query.replace(/"/g, '\\"')}"${type ? `, type: ${type.toUpperCase()}` : ''}) { id idMal title { romaji english native } coverImage { large extraLarge } bannerImage description genres tags { name } averageScore episodes duration status format season seasonYear studios { nodes { name } } characters { nodes { name { full } } } recommendations { nodes { mediaRecommendation { id title { romaji } coverImage { large } } } } relations { nodes { id title { romaji } coverImage { large } type } } } } }`
    const res = await fetch('https://graphql.anilist.co', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ query: gql }) })
    if (!res.ok) throw new Error('AniList search failed')
    const data = await res.json()
    return (data.data?.Page?.media || []).map(mapAniListMedia)
  },
  async getById(id: string): Promise<Title | null> {
    const gql = `query { Media(id: ${id}) { id idMal title { romaji english native } coverImage { large extraLarge } bannerImage description genres tags { name } averageScore episodes duration status format season seasonYear studios { nodes { name } } characters { nodes { name { full } } } recommendations { nodes { mediaRecommendation { id title { romaji } coverImage { large } } } } relations { nodes { id title { romaji } coverImage { large } type } } streamingEpisodes { title thumbnail url } } }`
    const res = await fetch('https://graphql.anilist.co', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ query: gql }) })
    if (!res.ok) return null
    const data = await res.json()
    return data.data?.Media ? mapAniListMedia(data.data.Media) : null
  },
  async getEpisodes(titleId: string): Promise<Episode[]> {
    const gql = `query { Media(id: ${titleId}) { streamingEpisodes { title thumbnail } episodes } }`
    const res = await fetch('https://graphql.anilist.co', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ query: gql }) })
    const data = await res.json()
    const eps = data.data?.Media?.streamingEpisodes || []
    return eps.map((e: any, i: number) => ({ id: `${titleId}-ep-${i+1}`, titleId, number: i + 1, title: e.title || `Episode ${i + 1}`, poster: e.thumbnail, sources: [] }))
  }
}

const tmdbProvider: MetadataProvider = {
  name: 'TMDB',
  priority: 2, weight: 1.0, latency: 0,
  async search(query: string, type?: string): Promise<Title[]> {
    const endpoint = type === 'anime' ? 'tv' : 'multi'
    const res = await fetch(`https://api.themoviedb.org/3/search/${endpoint}?api_key=demo&query=${encodeURIComponent(query)}&page=1`)
    if (!res.ok) throw new Error('TMDB search failed')
    const data = await res.json()
    return (data.results || []).map((m: any) => mapTMDBMedia(m, endpoint === 'tv' ? 'series' : m.media_type === 'movie' ? 'movie' : 'series'))
  },
  async getById(id: string): Promise<Title | null> {
    const [type, realId] = id.startsWith('tmdb-') ? id.split('-').slice(1) : ['movie', id]
    const res = await fetch(`https://api.themoviedb.org/3/${type}/${realId}?api_key=demo&append_to_response=credits,recommendations,similar`)
    if (!res.ok) return null
    const data = await res.json()
    return mapTMDBMedia(data, type === 'movie' ? 'movie' : 'series', true)
  },
  async getEpisodes(titleId: string, season?: number): Promise<Episode[]> {
    const realId = titleId.split('-')[1]
    const res = await fetch(`https://api.themoviedb.org/3/tv/${realId}/season/${season || 1}?api_key=demo`)
    if (!res.ok) return []
    const data = await res.json()
    return (data.episodes || []).map((e: any) => ({ id: `tmdb-${realId}-s${e.season_number}-e${e.episode_number}`, titleId, number: e.episode_number, season: e.season_number, title: e.name, overview: e.overview, poster: e.still_path ? `https://image.tmdb.org/t/p/w300${e.still_path}` : undefined, airDate: e.air_date, duration: e.runtime, sources: [] }))
  }
}

const simklProvider: MetadataProvider = {
  name: 'SIMKL',
  priority: 3, weight: 0.9, latency: 0,
  async search(query: string): Promise<Title[]> {
    const res = await fetch(`https://api.simkl.com/search/all?q=${encodeURIComponent(query)}&client_id=demo`)
    if (!res.ok) throw new Error('SIMKL search failed')
    const data = await res.json()
    return (data || []).map((m: any) => ({ id: `simkl-${m.ids?.simkl || m.id}`, title: m.title, type: m.type === 'movie' ? 'movie' : 'series', year: m.year, score: m.rating, poster: m.poster, banner: m.fanart, overview: m.overview, genres: m.genres || [], tags: [], cast: [], studios: [], directors: [], status: 'completed', sources: ['SIMKL'], ids: { simkl: m.ids?.simkl } }))
  },
  async getById(id: string): Promise<Title | null> {
    const realId = id.split('-')[1]
    const res = await fetch(`https://api.simkl.com/anime/${realId}?client_id=demo&extended=full`)
    if (!res.ok) return null
    const data = await res.json()
    return { id: `simkl-${data.ids?.simkl}`, title: data.title, type: data.type === 'movie' ? 'movie' : 'anime', year: data.year, score: data.rating, poster: data.poster, banner: data.fanart, overview: data.overview, genres: data.genres || [], tags: [], cast: (data.cast || []).map((c: any) => c.name), studios: (data.studios || []).map((s: any) => s.name), directors: [], episodeCount: data.total_episodes, status: data.status === 'airing' ? 'airing' : 'completed', sources: ['SIMKL'], ids: { simkl: data.ids?.simkl } }
  },
  async getEpisodes(titleId: string): Promise<Episode[]> {
    const realId = titleId.split('-')[1]
    const res = await fetch(`https://api.simkl.com/anime/episodes/${realId}?client_id=demo`)
    if (!res.ok) return []
    const data = await res.json()
    return (data.episodes || []).map((e: any) => ({ id: `simkl-${realId}-ep-${e.episode}`, titleId, number: e.episode, title: e.title, airDate: e.date, sources: [] }))
  }
}

const tvdbProvider: MetadataProvider = {
  name: 'TVDB',
  priority: 4, weight: 0.85, latency: 0,
  async search(query: string): Promise<Title[]> {
    const res = await fetch(`https://api4.thetvdb.com/v4/search?query=${encodeURIComponent(query)}&type=series`, { headers: { Authorization: 'Bearer demo' } })
    if (!res.ok) throw new Error('TVDB search failed')
    const data = await res.json()
    return (data.data || []).map((m: any) => ({ id: `tvdb-${m.id}`, title: m.name || m.translations?.eng || m.slug, type: 'series', year: m.year, score: m.score, poster: m.image, banner: m.banner, overview: m.overview, genres: m.genres || [], tags: [], cast: [], studios: [], directors: [], status: 'completed', sources: ['TVDB'], ids: { tvdb: m.id } }))
  },
  async getById(id: string): Promise<Title | null> {
    const realId = id.split('-')[1]
    const res = await fetch(`https://api4.thetvdb.com/v4/series/${realId}/extended`, { headers: { Authorization: 'Bearer demo' } })
    if (!res.ok) return null
    const data = await res.json()
    const m = data.data
    return { id: `tvdb-${m.id}`, title: m.name, type: 'series', year: m.year, score: m.score, poster: m.image, banner: m.banner, overview: m.overview, genres: m.genres?.map((g: any) => g.name) || [], tags: [], cast: (m.characters || []).map((c: any) => c.name), studios: [], directors: [], seasonCount: m.seasons?.length, status: m.status?.name === 'Continuing' ? 'airing' : 'completed', sources: ['TVDB'], ids: { tvdb: m.id } }
  },
  async getEpisodes(titleId: string, season?: number): Promise<Episode[]> {
    const realId = titleId.split('-')[1]
    const res = await fetch(`https://api4.thetvdb.com/v4/series/${realId}/episodes/default/${season || 1}?page=0`, { headers: { Authorization: 'Bearer demo' } })
    if (!res.ok) return []
    const data = await res.json()
    return (data.data?.episodes || []).map((e: any) => ({ id: `tvdb-${realId}-s${e.seasonNumber}-e${e.number}`, titleId, number: e.number, season: e.seasonNumber, title: e.name, overview: e.overview, airDate: e.aired, sources: [] }))
  }
}

const malProvider: MetadataProvider = {
  name: 'MAL',
  priority: 5, weight: 0.8, latency: 0,
  async search(query: string): Promise<Title[]> {
    const res = await fetch(`https://api.myanimelist.net/v2/anime?q=${encodeURIComponent(query)}&limit=20&fields=id,title,main_picture,alternative_titles,start_date,synopsis,mean,genres,num_episodes,studios,status`, { headers: { 'X-MAL-CLIENT-ID': 'demo' } })
    if (!res.ok) throw new Error('MAL search failed')
    const data = await res.json()
    return (data.data || []).map((item: any) => {
      const m = item.node
      return { id: `mal-${m.id}`, title: m.title, english: m.alternative_titles?.en, japanese: m.alternative_titles?.ja, type: 'anime', year: m.start_date ? parseInt(m.start_date) : undefined, score: m.mean, poster: m.main_picture?.large || m.main_picture?.medium, overview: m.synopsis, genres: (m.genres || []).map((g: any) => g.name), tags: [], cast: [], studios: (m.studios || []).map((s: any) => s.name), directors: [], episodeCount: m.num_episodes, status: m.status === 'currently_airing' ? 'airing' : m.status === 'finished_airing' ? 'completed' : 'upcoming', sources: ['MAL'], ids: { mal: m.id } }
    })
  },
  async getById(id: string): Promise<Title | null> {
    const realId = id.split('-')[1]
    const res = await fetch(`https://api.myanimelist.net/v2/anime/${realId}?fields=id,title,main_picture,alternative_titles,start_date,synopsis,mean,genres,num_episodes,studios,status,related_anime,recommendations`, { headers: { 'X-MAL-CLIENT-ID': 'demo' } })
    if (!res.ok) return null
    const m = await res.json()
    return { id: `mal-${m.id}`, title: m.title, english: m.alternative_titles?.en, japanese: m.alternative_titles?.ja, type: 'anime', year: m.start_date ? parseInt(m.start_date) : undefined, score: m.mean, poster: m.main_picture?.large, overview: m.synopsis, genres: (m.genres || []).map((g: any) => g.name), tags: [], cast: [], studios: (m.studios || []).map((s: any) => s.name), directors: [], episodeCount: m.num_episodes, status: m.status === 'currently_airing' ? 'airing' : m.status === 'finished_airing' ? 'completed' : 'upcoming', sources: ['MAL'], ids: { mal: m.id }, recommendations: (m.recommendations || []).slice(0, 10).map((r: any) => `mal-${r.node?.id}`).filter(Boolean), related: (m.related_anime || []).slice(0, 10).map((r: any) => `mal-${r.node?.id}`).filter(Boolean) }
  },
  async getEpisodes(titleId: string): Promise<Episode[]> {
    const realId = titleId.split('-')[1]
    const res = await fetch(`https://api.myanimelist.net/v2/anime/${realId}?fields=num_episodes`, { headers: { 'X-MAL-CLIENT-ID': 'demo' } })
    if (!res.ok) return []
    const data = await res.json()
    const count = data.num_episodes || 0
    return Array.from({ length: count }, (_, i) => ({ id: `mal-${realId}-ep-${i+1}`, titleId, number: i + 1, title: `Episode ${i + 1}`, sources: [] }))
  }
}

const kitsuProvider: MetadataProvider = {
  name: 'Kitsu',
  priority: 6, weight: 0.75, latency: 0,
  async search(query: string): Promise<Title[]> {
    const res = await fetch(`https://kitsu.io/api/edge/anime?filter[text]=${encodeURIComponent(query)}&page[limit]=20`)
    if (!res.ok) throw new Error('Kitsu search failed')
    const data = await res.json()
    return (data.data || []).map(mapKitsuMedia)
  },
  async getById(id: string): Promise<Title | null> {
    const realId = id.split('-')[1]
    const res = await fetch(`https://kitsu.io/api/edge/anime/${realId}`)
    if (!res.ok) return null
    const data = await res.json()
    return data.data ? mapKitsuMedia(data.data) : null
  },
  async getEpisodes(titleId: string): Promise<Episode[]> {
    const realId = titleId.split('-')[1]
    const res = await fetch(`https://kitsu.io/api/edge/anime/${realId}/episodes?page[limit]=20`)
    if (!res.ok) return []
    const data = await res.json()
    return (data.data || []).map((e: any, i: number) => ({ id: `kitsu-${realId}-ep-${i+1}`, titleId, number: e.attributes?.number || i + 1, title: e.attributes?.canonicalTitle || `Episode ${i + 1}`, poster: e.attributes?.thumbnail?.original, sources: [] }))
  }
}

const traktProvider: MetadataProvider = {
  name: 'Trakt',
  priority: 7, weight: 0.7, latency: 0,
  async search(query: string): Promise<Title[]> {
    const res = await fetch(`https://api.trakt.tv/search/movie,show?query=${encodeURIComponent(query)}&limit=20`, { headers: { 'trakt-api-version': '2', 'trakt-api-key': 'demo' } })
    if (!res.ok) throw new Error('Trakt search failed')
    const data = await res.json()
    return (data || []).map((item: any) => mapTraktMedia(item.movie || item.show))
  },
  async getById(id: string): Promise<Title | null> {
    const realId = id.split('-')[1]
    const res = await fetch(`https://api.trakt.tv/movies/${realId}?extended=full`, { headers: { 'trakt-api-version': '2', 'trakt-api-key': 'demo' } })
    if (!res.ok) return null
    const data = await res.json()
    return mapTraktMedia(data)
  },
  async getEpisodes(titleId: string): Promise<Episode[]> {
    const realId = titleId.split('-')[1]
    const res = await fetch(`https://api.trakt.tv/shows/${realId}/seasons?extended=episodes`, { headers: { 'trakt-api-version': '2', 'trakt-api-key': 'demo' } })
    if (!res.ok) return []
    const data = await res.json()
    const eps: Episode[] = []
    (data || []).forEach((season: any) => {
      (season.episodes || []).forEach((e: any) => {
        eps.push({ id: `trakt-${realId}-s${e.season}-e${e.number}`, titleId, number: e.number, season: e.season, title: e.title, airDate: e.first_aired, sources: [] })
      })
    })
    return eps
  }
}

const omdbProvider: MetadataProvider = {
  name: 'OMDB',
  priority: 8, weight: 0.65, latency: 0,
  async search(query: string): Promise<Title[]> {
    const res = await fetch(`https://www.omdbapi.com/?apikey=demo&s=${encodeURIComponent(query)}`)
    if (!res.ok) throw new Error('OMDB search failed')
    const data = await res.json()
    return (data.Search || []).map(mapOMDBMedia)
  },
  async getById(id: string): Promise<Title | null> {
    const realId = id.split('-')[1]
    const res = await fetch(`https://www.omdbapi.com/?apikey=demo&i=${realId}&plot=full`)
    if (!res.ok) return null
    const data = await res.json()
    return data.Response === 'True' ? mapOMDBMedia(data) : null
  },
  async getEpisodes(titleId: string): Promise<Episode[]> {
    const realId = titleId.split('-')[1]
    const res = await fetch(`https://www.omdbapi.com/?apikey=demo&i=${realId}&Season=1`)
    if (!res.ok) return []
    const data = await res.json()
    return (data.Episodes || []).map((e: any) => ({ id: `omdb-${realId}-e${e.Episode}`, titleId, number: parseInt(e.Episode), title: e.Title, sources: [] }))
  }
}

const fanartProvider: MetadataProvider = {
  name: 'FanArt',
  priority: 9, weight: 0.5, latency: 0,
  async search(): Promise<Title[]> { return [] },
  async getById(): Promise<Title | null> { return null },
  async getEpisodes(): Promise<Episode[]> { return [] }
}

const livechartProvider: MetadataProvider = {
  name: 'LiveChart',
  priority: 10, weight: 0.6, latency: 0,
  async search(query: string): Promise<Title[]> {
    const res = await fetch(`https://www.livechart.me/api/v1/anime?search=${encodeURIComponent(query)}&limit=20`)
    if (!res.ok) throw new Error('LiveChart search failed')
    const data = await res.json()
    return (data.anime || []).map((m: any) => ({ id: `livechart-${m.id}`, title: m.title, type: 'anime', year: m.premiere ? parseInt(m.premiere) : undefined, poster: m.poster?.url, banner: m.banner?.url, overview: m.synopsis, genres: m.genres || [], tags: [], cast: [], studios: (m.studios || []).map((s: any) => s.name), directors: [], episodeCount: m.episodes, status: m.status === 'airing' ? 'airing' : 'completed', sources: ['LiveChart'], ids: {} }))
  },
  async getById(id: string): Promise<Title | null> {
    const realId = id.split('-')[1]
    const res = await fetch(`https://www.livechart.me/api/v1/anime/${realId}`)
    if (!res.ok) return null
    const m = await res.json()
    return { id: `livechart-${m.id}`, title: m.title, type: 'anime', year: m.premiere ? parseInt(m.premiere) : undefined, poster: m.poster?.url, banner: m.banner?.url, overview: m.synopsis, genres: m.genres || [], tags: [], cast: [], studios: (m.studios || []).map((s: any) => s.name), directors: [], episodeCount: m.episodes, status: m.status === 'airing' ? 'airing' : 'completed', sources: ['LiveChart'], ids: {} }
  },
  async getEpisodes(): Promise<Episode[]> { return [] }
}

const ALL_PROVIDERS: MetadataProvider[] = [
  anilistProvider, tmdbProvider, simklProvider, tvdbProvider, 
  malProvider, kitsuProvider, traktProvider, omdbProvider, 
  fanartProvider, livechartProvider
]

export async function benchmarkProviders(): Promise<MetadataProvider[]> {
  const results = await Promise.allSettled(
    ALL_PROVIDERS.map(async (p) => {
      const start = performance.now()
      try { await p.search('naruto'); return { ...p, latency: performance.now() - start, weight: p.weight } }
      catch { return { ...p, latency: Infinity, weight: 0 } }
    })
  )
  const scored = results.map((r, i) => r.status === 'fulfilled' ? r.value : { ...ALL_PROVIDERS[i], latency: Infinity, weight: 0 }).sort((a, b) => { if (a.latency === Infinity) return 1; if (b.latency === Infinity) return -1; return a.latency - b.latency })
  console.log('[Arctic] Provider Benchmark:')
  scored.forEach(p => console.log(`  ${p.name}: ${p.latency === Infinity ? 'FAIL' : `${p.latency.toFixed(0)}ms`}`))
  return scored
}

let benchmarkedProviders: MetadataProvider[] | null = null
export async function getProviders(): Promise<MetadataProvider[]> {
  if (!benchmarkedProviders) benchmarkedProviders = await benchmarkProviders()
  return benchmarkedProviders
}

export async function unifiedSearch(query: string, type?: string): Promise<Title[]> {
  const providers = await getProviders()
  const results = await Promise.allSettled(providers.filter(p => p.weight > 0).map(p => p.search(query, type)))
  const allTitles: Title[] = []
  const seen = new Set<string>()
  results.forEach((r, i) => {
    if (r.status === 'fulfilled') {
      r.value.forEach(t => {
        const key = `${t.title.toLowerCase().trim()}-${t.year}-${t.type}`
        if (!seen.has(key)) { seen.add(key); allTitles.push({ ...t, sources: [providers[i].name] }) }
        else { const existing = allTitles.find(x => `${x.title.toLowerCase().trim()}-${x.year}-${x.type}` === key); if (existing && !existing.sources.includes(providers[i].name)) existing.sources.push(providers[i].name) }
      })
    }
  })
  return allTitles.sort((a, b) => (b.score || 0) - (a.score || 0))
}

export async function getTitleById(id: string): Promise<Title | null> {
  const providers = await getProviders()
  for (const p of providers) { if (p.weight === 0) continue; try { const r = await p.getById(id); if (r) return r } catch {} }
  return null
}

export async function getEpisodes(titleId: string, season?: number): Promise<Episode[]> {
  const providers = await getProviders()
  for (const p of providers) { if (p.weight === 0) continue; try { const r = await p.getEpisodes(titleId, season); if (r && r.length > 0) return r } catch {} }
  return []
}

export async function getTrending(type: 'anime' | 'movie' | 'series' = 'anime'): Promise<Title[]> {
  if (type === 'anime') {
    const gql = `query { Page(perPage: 20) { media(sort: TRENDING_DESC, type: ANIME) { id title { romaji english } coverImage { large } bannerImage averageScore genres status } } }`
    const res = await fetch('https://graphql.anilist.co', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ query: gql }) })
    const data = await res.json()
    return (data.data?.Page?.media || []).map(mapAniListMedia)
  }
  const res = await fetch(`https://api.themoviedb.org/3/trending/${type === 'movie' ? 'movie' : 'tv'}/week?api_key=demo`)
  const data = await res.json()
  return (data.results || []).map((m: any) => mapTMDBMedia(m, type === 'movie' ? 'movie' : 'series'))
}

export async function getPopular(type: 'anime' | 'movie' | 'series' = 'anime'): Promise<Title[]> {
  if (type === 'anime') {
    const gql = `query { Page(perPage: 20) { media(sort: POPULARITY_DESC, type: ANIME) { id title { romaji english } coverImage { large } bannerImage averageScore genres status } } }`
    const res = await fetch('https://graphql.anilist.co', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ query: gql }) })
    const data = await res.json()
    return (data.data?.Page?.media || []).map(mapAniListMedia)
  }
  const res = await fetch(`https://api.themoviedb.org/3/${type === 'movie' ? 'movie' : 'tv'}/popular?api_key=demo`)
  const data = await res.json()
  return (data.results || []).map((m: any) => mapTMDBMedia(m, type === 'movie' ? 'movie' : 'series'))
}

export async function getSeasonalAnime(season?: string, year?: number): Promise<Title[]> {
  const currentYear = year || new Date().getFullYear()
  const currentSeason = season || (() => {
    const month = new Date().getMonth() + 1
    if (month <= 3) return 'WINTER'
    if (month <= 6) return 'SPRING'
    if (month <= 9) return 'SUMMER'
    return 'FALL'
  })()
  const gql = `query { Page(perPage: 20) { media(season: ${currentSeason}, seasonYear: ${currentYear}, type: ANIME, sort: POPULARITY_DESC) { id title { romaji english } coverImage { large } bannerImage averageScore genres status } } }`
  const res = await fetch('https://graphql.anilist.co', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ query: gql }) })
  const data = await res.json()
  return (data.data?.Page?.media || []).map(mapAniListMedia)
}

export async function getUpcoming(type: 'anime' | 'movie' | 'series' = 'anime'): Promise<Title[]> {
  if (type === 'anime') {
    const gql = `query { Page(perPage: 20) { media(status: NOT_YET_RELEASED, type: ANIME, sort: POPULARITY_DESC) { id title { romaji english } coverImage { large } bannerImage averageScore genres status startDate { year month day } } } }`
    const res = await fetch('https://graphql.anilist.co', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ query: gql }) })
    const data = await res.json()
    return (data.data?.Page?.media || []).map(mapAniListMedia)
  }
  const res = await fetch(`https://api.themoviedb.org/3/${type === 'movie' ? 'movie' : 'tv'}/upcoming?api_key=demo`)
  const data = await res.json()
  return (data.results || []).map((m: any) => mapTMDBMedia(m, type === 'movie' ? 'movie' : 'series'))
}
