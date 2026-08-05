import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'
import type { Title, Profile, UserPreferences, ExtensionManifest, DownloadItem, WatchHistory, Playlist } from './types'

interface AppState {
  // Profiles
  profiles: Profile[]
  activeProfileId: string | null
  setActiveProfile: (id: string) => void
  addProfile: (profile: Profile) => void
  updateProfile: (id: string, updates: Partial<Profile>) => void
  removeProfile: (id: string) => void

  // Library
  library: string[]
  addToLibrary: (titleId: string) => void
  removeFromLibrary: (titleId: string) => void
  isInLibrary: (titleId: string) => boolean

  // Watch History
  watchHistory: WatchHistory[]
  addWatchHistory: (entry: WatchHistory) => void
  getWatchProgress: (titleId: string, episodeId?: string) => number

  // Search
  searchQuery: string
  searchResults: Title[]
  searchLoading: boolean
  setSearchQuery: (q: string) => void
  setSearchResults: (results: Title[]) => void
  setSearchLoading: (loading: boolean) => void
  searchHistory: string[]
  addSearchHistory: (q: string) => void

  // Extensions
  extensions: ExtensionManifest[]
  installExtension: (ext: ExtensionManifest) => void
  uninstallExtension: (id: string) => void
  toggleExtension: (id: string) => void

  // Downloads
  downloads: DownloadItem[]
  addDownload: (item: DownloadItem) => void
  updateDownload: (id: string, updates: Partial<DownloadItem>) => void
  removeDownload: (id: string) => void

  // Player
  currentTitle: Title | null
  currentEpisode: any | null
  isPlaying: boolean
  isFullscreen: boolean
  setCurrentTitle: (title: Title | null) => void
  setCurrentEpisode: (ep: any | null) => void
  setIsPlaying: (playing: boolean) => void
  setIsFullscreen: (fs: boolean) => void

  // UI
  sidebarOpen: boolean
  tvMode: boolean
  currentPage: string
  setSidebarOpen: (open: boolean) => void
  setTvMode: (tv: boolean) => void
  setCurrentPage: (page: string) => void

  // Settings
  preferences: UserPreferences
  updatePreferences: (prefs: Partial<UserPreferences>) => void

  // Sync
  lastSync: number
  setLastSync: (t: number) => void

  // Memory
  memoryPressure: 'normal' | 'warning' | 'critical'
  setMemoryPressure: (level: 'normal' | 'warning' | 'critical') => void
}

const defaultPreferences: UserPreferences = {
  preferEnglishTitles: true,
  defaultQuality: 'auto',
  defaultAudio: 'en',
  defaultSubtitle: 'en',
  autoSkipIntro: true,
  autoSkipOutro: false,
  autoNextEpisode: true,
  reduceMotion: false,
  cardShape: 'poster',
  showRatings: true,
  density: 'normal',
  theme: 'arctic',
  deviceProfile: 'DESKTOP',
}

export const useStore = create<AppState>()(
  persist(
    (set, get) => ({
      profiles: [],
      activeProfileId: null,
      setActiveProfile: (id) => set({ activeProfileId: id }),
      addProfile: (p) => set((s) => ({ profiles: [...s.profiles, p] })),
      updateProfile: (id, updates) => set((s) => ({
        profiles: s.profiles.map(p => p.id === id ? { ...p, ...updates } : p)
      })),
      removeProfile: (id) => set((s) => ({
        profiles: s.profiles.filter(p => p.id !== id),
        activeProfileId: s.activeProfileId === id ? null : s.activeProfileId
      })),

      library: [],
      addToLibrary: (id) => set((s) => ({ library: [...new Set([...s.library, id])] })),
      removeFromLibrary: (id) => set((s) => ({ library: s.library.filter(x => x !== id) })),
      isInLibrary: (id) => get().library.includes(id),

      watchHistory: [],
      addWatchHistory: (entry) => set((s) => {
        const filtered = s.watchHistory.filter(h => !(h.titleId === entry.titleId && h.episodeId === entry.episodeId))
        return { watchHistory: [...filtered, entry].slice(-500) }
      }),
      getWatchProgress: (titleId, episodeId) => {
        const entry = get().watchHistory.find(h => h.titleId === titleId && h.episodeId === episodeId)
        return entry ? entry.progress : 0
      },

      searchQuery: '',
      searchResults: [],
      searchLoading: false,
      setSearchQuery: (q) => set({ searchQuery: q }),
      setSearchResults: (r) => set({ searchResults: r }),
      setSearchLoading: (l) => set({ searchLoading: l }),
      searchHistory: [],
      addSearchHistory: (q) => set((s) => ({ searchHistory: [...new Set([q, ...s.searchHistory])].slice(0, 20) })),

      extensions: [],
      installExtension: (ext) => set((s) => {
        const filtered = s.extensions.filter(e => e.id !== ext.id)
        return { extensions: [...filtered, { ...ext, installed: true, enabled: true }] }
      }),
      uninstallExtension: (id) => set((s) => ({
        extensions: s.extensions.filter(e => e.id !== id)
      })),
      toggleExtension: (id) => set((s) => ({
        extensions: s.extensions.map(e => e.id === id ? { ...e, enabled: !e.enabled } : e)
      })),

      downloads: [],
      addDownload: (item) => set((s) => ({ downloads: [...s.downloads, item] })),
      updateDownload: (id, updates) => set((s) => ({
        downloads: s.downloads.map(d => d.id === id ? { ...d, ...updates } : d)
      })),
      removeDownload: (id) => set((s) => ({
        downloads: s.downloads.filter(d => d.id !== id)
      })),

      currentTitle: null,
      currentEpisode: null,
      isPlaying: false,
      isFullscreen: false,
      setCurrentTitle: (t) => set({ currentTitle: t }),
      setCurrentEpisode: (e) => set({ currentEpisode: e }),
      setIsPlaying: (p) => set({ isPlaying: p }),
      setIsFullscreen: (f) => set({ isFullscreen: f }),

      sidebarOpen: false,
      tvMode: false,
      currentPage: 'home',
      setSidebarOpen: (o) => set({ sidebarOpen: o }),
      setTvMode: (t) => set({ tvMode: t }),
      setCurrentPage: (p) => set({ currentPage: p }),

      preferences: defaultPreferences,
      updatePreferences: (prefs) => set((s) => ({ preferences: { ...s.preferences, ...prefs } })),

      lastSync: 0,
      setLastSync: (t) => set({ lastSync: t }),

      memoryPressure: 'normal',
      setMemoryPressure: (level) => set({ memoryPressure: level }),
    }),
    {
      name: 'arctic-aio-storage',
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        profiles: state.profiles,
        activeProfileId: state.activeProfileId,
        library: state.library,
        watchHistory: state.watchHistory,
        searchHistory: state.searchHistory,
        extensions: state.extensions,
        downloads: state.downloads,
        preferences: state.preferences,
        lastSync: state.lastSync,
      }),
    }
  )
)
