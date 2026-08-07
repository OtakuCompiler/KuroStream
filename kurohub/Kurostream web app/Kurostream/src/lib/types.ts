export interface Title {
  id: string
  title: string
  english?: string
  japanese?: string
  type: 'movie' | 'series' | 'anime'
  year?: number
  score?: number
  poster?: string
  banner?: string
  backdrop?: string
  overview?: string
  genres: string[]
  tags: string[]
  cast: string[]
  studios: string[]
  directors: string[]
  duration?: number
  seasonCount?: number
  episodeCount?: number
  status: 'airing' | 'completed' | 'upcoming' | 'cancelled'
  sources: string[]
  ids: {
    anilist?: number
    mal?: number
    tmdb?: number
    imdb?: string
    tvdb?: number
    kitsu?: string | number
    trakt?: number
    simkl?: number
  }
  episodes?: Episode[]
  recommendations?: string[]
  related?: string[]
}

export interface Episode {
  id: string
  titleId: string
  number: number
  season?: number
  title: string
  overview?: string
  poster?: string
  airDate?: string
  duration?: number
  sources: Source[]
  watched?: boolean
  progress?: number
}

export interface Source {
  id: string
  url: string
  quality: '4K' | '1080p' | '720p' | '480p' | '360p' | 'unknown'
  provider: string
  type: 'hls' | 'dash' | 'mp4' | 'webm' | 'torrent' | 'direct'
  size?: number
  seeds?: number
  peers?: number
  audioLang: string[]
  subtitleLang: string[]
  codec?: string
  hdr?: boolean
  score: number
}

export interface SubtitleTrack {
  id: string
  label: string
  language: string
  url: string
  format: 'srt' | 'ass' | 'vtt'
  isDefault?: boolean
  isSDH?: boolean
  isForced?: boolean
}

export interface WatchHistory {
  titleId: string
  episodeId?: string
  progress: number
  duration: number
  completed: boolean
  timestamp: number
  watchCount: number
}

export interface Profile {
  id: string
  name: string
  avatar?: string
  isKids: boolean
  pin?: string
  preferences: UserPreferences
  watchHistory: WatchHistory[]
  library: string[]
  playlists: Playlist[]
}

export interface UserPreferences {
  preferEnglishTitles: boolean
  defaultQuality: 'auto' | '4K' | '1080p' | '720p' | '480p'
  defaultAudio: string
  defaultSubtitle: string
  autoSkipIntro: boolean
  autoSkipOutro: boolean
  autoNextEpisode: boolean
  reduceMotion: boolean
  cardShape: 'poster' | 'wide' | 'square'
  showRatings: boolean
  density: 'compact' | 'normal' | 'comfortable'
  theme: 'dark' | 'darker' | 'midnight' | 'arctic'
  deviceProfile: 'LOW' | 'TV' | 'MOBILE' | 'DESKTOP'
}

export interface Playlist {
  id: string
  name: string
  items: string[]
  createdAt: number
}

export interface ExtensionManifest {
  id: string
  name: string
  version: string
  author: string
  description: string
  type: 'provider' | 'metadata' | 'subtitle' | 'theme' | 'widget'
  entry: string
  permissions: string[]
  icon?: string
  rating?: number
  installed: boolean
  enabled: boolean
  updatedAt?: number
}

export interface DownloadItem {
  id: string
  title: string
  url: string
  type: 'http' | 'torrent'
  progress: number
  status: 'queued' | 'downloading' | 'paused' | 'completed' | 'error'
  speed: number
  size: number
  downloaded: number
  path: string
  createdAt: number
  peers?: number
}

export interface IntroSkipData {
  introStart: number
  introEnd: number
  outroStart: number
  outroEnd: number
  confidence: number
}

export interface PlaybackPreset {
  id: string
  name: string
  brightness: number
  contrast: number
  saturation: number
  sharpness: number
  denoise: boolean
  upscaling: 'none' | 'lanczos' | 'fsr' | 'cas'
}
