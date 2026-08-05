import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { Play, Plus, Check, Info, Star, TrendingUp, Calendar, Clock } from 'lucide-react'
import { cn } from '@/lib/utils'
import { useStore } from '@/lib/store'
import type { Title } from '@/lib/types'

interface HeroProps {
  title: Title | undefined
  loading?: boolean
}

export function Hero({ title, loading }: HeroProps) {
  const { preferences, addToLibrary, removeFromLibrary, isInLibrary } = useStore()
  const [imageLoaded, setImageLoaded] = useState(false)

  if (loading || !title) {
    return (
      <section className="relative mx-4 sm:mx-8 h-[50vh] sm:h-[60vh] rounded-3xl overflow-hidden">
        <div className="absolute inset-0 fuse-shimmer" />
      </section>
    )
  }

  const label = preferences.preferEnglishTitles ? title.english || title.title : title.title
  const inLib = isInLibrary(title.id)

  return (
    <section className="relative mx-4 sm:mx-8 rounded-3xl overflow-hidden border border-border shadow-fuse group">
      {/* Background */}
      <div className="absolute inset-0">
        {title.banner ? (
          <>
            <img
              src={title.banner}
              alt=""
              fetchPriority="high"
              decoding="async"
              className={cn(
                "w-full h-full object-cover transition-opacity duration-700",
                imageLoaded ? "opacity-100" : "opacity-0"
              )}
              onLoad={() => setImageLoaded(true)}
            />
            {!imageLoaded && <div className="absolute inset-0 fuse-shimmer" />}
          </>
        ) : (
          <div className="w-full h-full bg-gradient-to-br from-surface-elevated to-background" />
        )}

        {/* Gradient Overlays - Arctic Fuse 3 Style */}
        <div className="absolute inset-0 bg-gradient-to-r from-background via-background/85 to-transparent" />
        <div className="absolute inset-0 bg-gradient-to-t from-background via-transparent to-background/30" />
        <div className="absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-accent/5" />
      </div>

      {/* Content */}
      <div className="relative flex flex-col justify-end min-h-[50vh] sm:min-h-[60vh] p-6 sm:p-10 lg:p-14 gap-4">
        {/* Badges */}
        <div className="flex flex-wrap items-center gap-2">
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-primary/15 text-primary text-xs font-semibold uppercase tracking-wider">
            <TrendingUp className="w-3 h-3" />
            Featured
          </span>
          {title.score && (
            <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full fuse-glass text-xs font-semibold">
              <Star className="w-3 h-3 fill-gold text-gold" />
              {title.score.toFixed(1)}
            </span>
          )}
          {title.year && (
            <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full fuse-glass text-xs text-text-muted">
              <Calendar className="w-3 h-3" />
              {title.year}
            </span>
          )}
          {title.duration && (
            <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full fuse-glass text-xs text-text-muted">
              <Clock className="w-3 h-3" />
              {Math.floor(title.duration / 60)}h {title.duration % 60}m
            </span>
          )}
          {title.sources.map(s => (
            <span key={s} className="px-2 py-0.5 rounded-full bg-white/5 text-[10px] uppercase tracking-wider text-text-dim">
              {s}
            </span>
          ))}
        </div>

        {/* Title */}
        <h1 className="max-w-2xl text-3xl sm:text-5xl lg:text-6xl font-bold text-glow leading-tight">
          {label}
        </h1>

        {/* Meta */}
        <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-text-muted">
          {title.genres.slice(0, 4).map(g => (
            <span key={g} className="text-text">{g}</span>
          ))}
          {title.studios[0] && (
            <span className="text-text-dim">{title.studios[0]}</span>
          )}
          {title.status === 'airing' && (
            <span className="px-2 py-0.5 rounded-full bg-success/15 text-success text-xs font-medium">Airing</span>
          )}
        </div>

        {/* Overview */}
        <p className="max-w-xl text-sm sm:text-base text-text-muted line-clamp-3 leading-relaxed">
          {title.overview}
        </p>

        {/* Actions */}
        <div className="flex flex-wrap items-center gap-3 pt-2">
          <Link
            to={`/watch/${title.id}`}
            className="inline-flex items-center gap-2 px-6 py-3 rounded-xl bg-primary text-white font-semibold text-sm hover:bg-primary/90 transition-all shadow-glow hover:shadow-lg hover:scale-[1.02] active:scale-[0.98]"
          >
            <Play className="w-4 h-4 fill-white" />
            Play Now
          </Link>

          <button
            onClick={() => inLib ? removeFromLibrary(title.id) : addToLibrary(title.id)}
            className={cn(
              "inline-flex items-center gap-2 px-5 py-3 rounded-xl font-semibold text-sm transition-all hover:scale-[1.02] active:scale-[0.98]",
              inLib 
                ? "bg-success/15 text-success border border-success/20" 
                : "bg-white/10 text-white hover:bg-white/15 border border-white/10"
            )}
          >
            {inLib ? <Check className="w-4 h-4" /> : <Plus className="w-4 h-4" />}
            {inLib ? 'In Library' : 'Add to Library'}
          </button>

          <Link
            to={`/title/${title.id}`}
            className="inline-flex items-center gap-2 px-5 py-3 rounded-xl bg-white/5 text-text-muted font-semibold text-sm hover:bg-white/10 transition-all border border-white/5"
          >
            <Info className="w-4 h-4" />
            More Info
          </Link>
        </div>

        {/* Cast Preview */}
        {title.cast.length > 0 && (
          <div className="flex items-center gap-2 pt-1">
            <span className="text-xs text-text-dim">Starring:</span>
            <div className="flex -space-x-2">
              {title.cast.slice(0, 4).map((actor, i) => (
                <div
                  key={i}
                  className="w-7 h-7 rounded-full bg-surface-elevated border-2 border-background flex items-center justify-center text-[10px] font-bold text-text-muted"
                  title={actor}
                >
                  {actor.split(' ').map(n => n[0]).join('')}
                </div>
              ))}
            </div>
            <span className="text-xs text-text-dim">{title.cast.slice(0, 3).join(', ')}{title.cast.length > 3 && '...'}</span>
          </div>
        )}
      </div>
    </section>
  )
}
