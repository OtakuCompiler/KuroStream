import { useState, useRef, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { Search, X, Mic, Clock, TrendingUp, Sparkles } from 'lucide-react'
import { cn, debounce, fuzzyMatch } from '@/lib/utils'
import { useStore } from '@/lib/store'
import { unifiedSearch } from '@/lib/metadata/engine'
import { MediaCard, CardSkeleton } from './MediaCard'
import type { Title } from '@/lib/types'

export function GlobalSearch() {
  const navigate = useNavigate()
  const { searchHistory, addSearchHistory, setSearchQuery, setSearchResults, searchResults, searchLoading, setSearchLoading } = useStore()
  const [query, setQuery] = useState('')
  const [isOpen, setIsOpen] = useState(false)
  const [suggestions, setSuggestions] = useState<string[]>([])
  const [isListening, setIsListening] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)
  const containerRef = useRef<HTMLDivElement>(null)

  const performSearch = useCallback(async (q: string) => {
    if (!q.trim()) return
    setSearchLoading(true)
    setSearchQuery(q)
    addSearchHistory(q)
    try {
      const results = await unifiedSearch(q)
      setSearchResults(results)
    } catch (e) {
      console.error('Search error:', e)
    } finally {
      setSearchLoading(false)
    }
  }, [addSearchHistory, setSearchQuery, setSearchResults, setSearchLoading])

  const debouncedSearch = useCallback(debounce((q: string) => {
    if (q.length >= 2) performSearch(q)
  }, 400), [performSearch])

  useEffect(() => {
    debouncedSearch(query)
  }, [query, debouncedSearch])

  // Voice search
  const startVoiceSearch = () => {
    if (!('webkitSpeechRecognition' in window)) return
    const recognition = new (window as any).webkitSpeechRecognition()
    recognition.lang = 'en-US'
    recognition.continuous = false
    recognition.interimResults = false

    recognition.onstart = () => setIsListening(true)
    recognition.onend = () => setIsListening(false)
    recognition.onresult = (e: any) => {
      const transcript = e.results[0][0].transcript
      setQuery(transcript)
      performSearch(transcript)
    }

    recognition.start()
  }

  // Keyboard shortcut
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === '/' && document.activeElement?.tagName !== 'INPUT') {
        e.preventDefault()
        inputRef.current?.focus()
        setIsOpen(true)
      }
      if (e.key === 'Escape') {
        setIsOpen(false)
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [])

  // Click outside
  useEffect(() => {
    const onClick = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false)
      }
    }
    document.addEventListener('mousedown', onClick)
    return () => document.removeEventListener('mousedown', onClick)
  }, [])

  return (
    <div ref={containerRef} className="relative w-full max-w-2xl mx-auto">
      {/* Search Input */}
      <div className={cn(
        "relative flex items-center gap-2 px-4 py-2.5 rounded-2xl border transition-all duration-200",
        isOpen 
          ? "bg-surface-elevated border-primary/30 shadow-glow" 
          : "bg-surface border-border hover:border-border-strong"
      )}>
        <Search className={cn("w-4 h-4 flex-shrink-0", isOpen ? "text-primary" : "text-text-dim")} />
        <input
          ref={inputRef}
          type="text"
          value={query}
          onChange={e => { setQuery(e.target.value); setIsOpen(true) }}
          onFocus={() => setIsOpen(true)}
          placeholder="Search movies, shows, anime... (Press / to focus)"
          className="flex-1 bg-transparent text-sm text-text placeholder:text-text-dim outline-none"
        />
        {query && (
          <button onClick={() => { setQuery(''); setSearchResults([]) }} className="p-1 rounded-lg hover:bg-white/5 transition-colors">
            <X className="w-3.5 h-3.5 text-text-dim" />
          </button>
        )}
        <button 
          onClick={startVoiceSearch}
          className={cn(
            "p-1.5 rounded-lg transition-colors",
            isListening ? "bg-danger/20 text-danger animate-pulse" : "hover:bg-white/5 text-text-dim"
          )}
        >
          <Mic className="w-3.5 h-3.5" />
        </button>
      </div>

      {/* Dropdown */}
      {isOpen && (
        <div className="absolute top-full left-0 right-0 mt-2 rounded-2xl fuse-glass-strong border border-border shadow-fuse max-h-[70vh] overflow-y-auto z-50">
          {/* Search History */}
          {!query && searchHistory.length > 0 && (
            <div className="p-4">
              <div className="flex items-center gap-2 mb-3">
                <Clock className="w-3.5 h-3.5 text-text-dim" />
                <span className="text-xs font-semibold text-text-dim uppercase tracking-wider">Recent</span>
              </div>
              <div className="flex flex-wrap gap-2">
                {searchHistory.slice(0, 8).map((h, i) => (
                  <button
                    key={i}
                    onClick={() => { setQuery(h); performSearch(h) }}
                    className="px-3 py-1.5 rounded-lg bg-white/5 text-sm text-text-muted hover:bg-white/10 hover:text-text transition-colors"
                  >
                    {h}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Results */}
          {query.length >= 2 && (
            <div className="p-4">
              {searchLoading ? (
                <div className="flex gap-3 overflow-x-auto pb-2">
                  {Array.from({ length: 5 }).map((_, i) => (
                    <CardSkeleton key={i} size="sm" />
                  ))}
                </div>
              ) : searchResults.length > 0 ? (
                <>
                  <div className="flex items-center gap-2 mb-3">
                    <Sparkles className="w-3.5 h-3.5 text-primary" />
                    <span className="text-xs font-semibold text-text-dim uppercase tracking-wider">
                      {searchResults.length} Results
                    </span>
                  </div>
                  <div className="flex gap-3 overflow-x-auto pb-2 arctic-scrollbar-hide">
                    {searchResults.slice(0, 15).map(item => (
                      <div key={item.id} className="flex-shrink-0">
                        <MediaCard title={item} size="sm" />
                      </div>
                    ))}
                  </div>
                </>
              ) : (
                <div className="text-center py-8 text-text-dim text-sm">
                  No results found for "{query}"
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  )
}
