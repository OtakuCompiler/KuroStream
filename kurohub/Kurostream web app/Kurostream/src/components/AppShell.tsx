import { useState, useEffect, useRef } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { 
  Home, Compass, Library, Play, Search, Settings, 
  Grid3X3, Download, Puzzle, User, Moon, Sun,
  ChevronLeft, ChevronRight, Monitor, Smartphone, Tv
} from 'lucide-react'
import { cn, getDeviceProfile } from '@/lib/utils'
import { useStore } from '@/lib/store'

const HUBS = [
  { id: 'home', label: 'Home', icon: Home, path: '/' },
  { id: 'browse', label: 'Browse', icon: Compass, path: '/browse' },
  { id: 'search', label: 'Search', icon: Search, path: '/search' },
  { id: 'library', label: 'Library', icon: Library, path: '/library' },
  { id: 'extensions', label: 'Extensions', icon: Puzzle, path: '/extensions' },
  { id: 'downloads', label: 'Downloads', icon: Download, path: '/downloads' },
  { id: 'settings', label: 'Settings', icon: Settings, path: '/settings' },
]

export function AppShell({ children }: { children: React.ReactNode }) {
  const location = useLocation()
  const navigate = useNavigate()
  const { tvMode, setTvMode, preferences, activeProfileId, profiles } = useStore()
  const [activeHub, setActiveHub] = useState(0)
  const [showHubSwitcher, setShowHubSwitcher] = useState(true)
  const [focusedIndex, setFocusedIndex] = useState(0)
  const navRef = useRef<HTMLDivElement>(null)

  const activeProfile = profiles.find(p => p.id === activeProfileId)

  // TV Navigation
  useEffect(() => {
    if (!tvMode) return

    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'ArrowLeft') {
        e.preventDefault()
        setActiveHub(prev => Math.max(0, prev - 1))
      }
      if (e.key === 'ArrowRight') {
        e.preventDefault()
        setActiveHub(prev => Math.min(HUBS.length - 1, prev + 1))
      }
      if (e.key === 'ArrowUp') {
        e.preventDefault()
        setShowHubSwitcher(true)
      }
      if (e.key === 'ArrowDown') {
        e.preventDefault()
        setShowHubSwitcher(false)
      }
      if (e.key === 'Enter') {
        e.preventDefault()
        navigate(HUBS[activeHub].path)
      }
      if (e.key === 'Backspace') {
        e.preventDefault()
        navigate(-1)
      }
    }

    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [tvMode, activeHub, navigate])

  // Auto-hide hub switcher
  useEffect(() => {
    if (!tvMode) return
    const timer = setTimeout(() => setShowHubSwitcher(false), 5000)
    return () => clearTimeout(timer)
  }, [tvMode, activeHub])

  const currentHubIndex = HUBS.findIndex(h => h.path === location.pathname) || 0

  return (
    <div className={cn("min-h-screen bg-background text-text", tvMode && "tv-mode")}>
      {/* Hub Switcher - Arctic Fuse 3 Style */}
      <header className={cn(
        "fixed top-0 left-0 right-0 z-50 transition-all duration-500",
        tvMode ? (showHubSwitcher ? "translate-y-0" : "-translate-y-full") : "translate-y-0"
      )}>
        <div className="fuse-glass-strong px-6 py-3">
          <div className="flex items-center justify-between max-w-[1920px] mx-auto">
            {/* Logo */}
            <Link to="/" className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-primary to-accent flex items-center justify-center shadow-glow">
                <span className="text-white font-bold text-sm">A</span>
              </div>
              <span className="font-semibold text-lg tracking-tight hidden sm:block">Arctic AIO</span>
            </Link>

            {/* Hub Navigation */}
            <nav className="flex items-center gap-1" ref={navRef}>
              {HUBS.map((hub, i) => {
                const Icon = hub.icon
                const isActive = location.pathname === hub.path || (hub.path !== '/' && location.pathname.startsWith(hub.path))
                const isFocused = tvMode && i === activeHub

                return (
                  <Link
                    key={hub.id}
                    to={hub.path}
                    onClick={() => setActiveHub(i)}
                    className={cn(
                      "relative flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium transition-all duration-200",
                      isActive ? "text-white bg-primary/15" : "text-text-muted hover:text-text hover:bg-white/5",
                      isFocused && tvMode && "ring-2 ring-primary/60 ring-offset-2 ring-offset-background scale-105"
                    )}
                  >
                    <Icon className="w-4 h-4" />
                    <span className="hidden md:inline">{hub.label}</span>
                    {isActive && (
                      <div className="absolute bottom-0 left-1/2 -translate-x-1/2 w-1 h-1 rounded-full bg-primary" />
                    )}
                  </Link>
                )
              })}
            </nav>

            {/* Right Actions */}
            <div className="flex items-center gap-2">
              {/* Device Profile Indicator */}
              <div className="hidden lg:flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg bg-white/5 text-xs text-text-muted">
                {preferences.deviceProfile === 'TV' && <Tv className="w-3.5 h-3.5" />}
                {preferences.deviceProfile === 'MOBILE' && <Smartphone className="w-3.5 h-3.5" />}
                {preferences.deviceProfile === 'DESKTOP' && <Monitor className="w-3.5 h-3.5" />}
                <span className="uppercase tracking-wider">{preferences.deviceProfile}</span>
              </div>

              {/* TV Mode Toggle */}
              <button
                onClick={() => setTvMode(!tvMode)}
                className={cn(
                  "p-2 rounded-xl transition-colors",
                  tvMode ? "bg-primary/20 text-primary" : "hover:bg-white/5 text-text-muted"
                )}
                title="TV Mode"
              >
                <Tv className="w-4 h-4" />
              </button>

              {/* Profile */}
              <Link to="/settings" className="flex items-center gap-2 pl-2 pr-3 py-1.5 rounded-xl hover:bg-white/5 transition-colors">
                <div className="w-7 h-7 rounded-full bg-gradient-to-br from-accent to-primary flex items-center justify-center text-xs font-bold">
                  {activeProfile?.name?.[0] || 'G'}
                </div>
                <span className="text-sm hidden sm:block">{activeProfile?.name || 'Guest'}</span>
              </Link>
            </div>
          </div>
        </div>

        {/* Date/Time - Arctic Fuse 3 Style */}
        {tvMode && showHubSwitcher && (
          <div className="absolute top-full right-6 mt-2 text-xs text-text-dim font-mono">
            {new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
            {' · '}
            {new Date().toLocaleDateString([], { weekday: 'short', month: 'short', day: 'numeric' })}
          </div>
        )}
      </header>

      {/* Main Content */}
      <main className={cn(
        "pt-16 transition-all",
        tvMode && "pt-20"
      )}>
        {children}
      </main>
    </div>
  )
}
