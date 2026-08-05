import { useState, memo } from 'react'
import { Link } from 'react-router-dom'
import { Star, Play, Check, Plus } from 'lucide-react'
import { cn } from '@/lib/utils'
import { useStore } from '@/lib/store'
import type { Title } from '@/lib/types'

interface MediaCardProps {
  title: Title
  priority?: boolean
  size?: 'sm' | 'md' | 'lg' | 'xl'
  shape?: 'poster' | 'wide' | 'square'
  showMeta?: boolean
}

const SIZE_CLASSES = {
  sm: 'w-[120px] sm:w-[140px]',
  md: 'w-[150px] sm:w-[180px]',
  lg: 'w-[180px] sm:w-[220px]',
  xl: 'w-[240px] sm:w-[300px]',
}

const ASPECT_CLASSES = {
  poster: 'aspect-[2/3]',
  wide: 'aspect-video',
  square: 'aspect-square',
}

export const MediaCard = memo(function MediaCard({ title, priority = false, size = 'md', shape, showMeta = true }: MediaCardProps) {
  const { preferences, isInLibrary, addToLibrary, removeFromLibrary } = useStore()
  const [imageLoaded, setImageLoaded] = useState(false)
  const [isHovered, setIsHovered] = useState(false)

  const cardShape = shape || preferences.cardShape
  const label = preferences.preferEnglishTitles ? title.english || title.title : title.title
  const inLib = isInLibrary(title.id)

  return (
    <div 
      className={cn("group relative flex-shrink-0", SIZE_CLASSES[size])}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
    >
      <Link to={`/title/${title.id}`} className="block">
        <div className={cn(
          "relative overflow-hidden rounded-2xl bg-surface shadow-fuse card-hover",
          ASPECT_CLASSES[cardShape]
        )}>
          {/* Poster */}
          {title.poster ? (
            <>
              <img
                src={title.poster}
                alt={label}
                loading={priority ? 'eager' : 'lazy'}
                decoding="async"
                fetchPriority={priority ? 'high' : 'low'}
                className={cn(
                  "w-full h-full object-cover transition-all duration-500",
                  imageLoaded ? "opacity-100" : "opacity-0",
                  isHovered && "scale-110"
                )}
                onLoad={() => setImageLoaded(true)}
              />
              {!imageLoaded && <div className="absolute inset-0 fuse-shimmer" />}
            </>
          ) : (
            <div className="w-full h-full fuse-shimmer flex items-center justify-center">
              <span className="text-4xl font-bold text-text-dim/30">{label[0]}</span>
            </div>
          )}

          {/* Hover Overlay */}
          <div className={cn(
            "absolute inset-0 bg-gradient-to-t from-black/90 via-black/20 to-transparent transition-opacity duration-300 flex flex-col justify-end p-3",
            isHovered ? "opacity-100" : "opacity-0"
          )}>
            <div className="flex items-center gap-2">
              <Link 
                to={`/watch/${title.id}`}
                className="p-2 rounded-full bg-primary/90 text-white hover:bg-primary transition-colors"
                onClick={e => e.stopPropagation()}
              >
                <Play className="w-4 h-4 fill-white" />
              </Link>
              <button
                onClick={e => { e.preventDefault(); e.stopPropagation(); inLib ? removeFromLibrary(title.id) : addToLibrary(title.id) }}
                className={cn(
                  "p-2 rounded-full transition-colors",
                  inLib ? "bg-success/90 text-white" : "bg-white/20 text-white hover:bg-white/30"
                )}
              >
                {inLib ? <Check className="w-4 h-4" /> : <Plus className="w-4 h-4" />}
              </button>
            </div>
          </div>

          {/* Top Badges */}
          <div className="absolute top-2 left-2 right-2 flex justify-between pointer-events-none">
            {title.score && preferences.showRatings && (
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full fuse-glass text-[11px] font-semibold">
                <Star className="w-3 h-3 fill-gold text-gold" />
                {title.score.toFixed(1)}
              </span>
            )}
            {title.sources.length > 1 && (
              <span className="px-2 py-0.5 rounded-full fuse-glass text-[10px] font-medium uppercase tracking-wide text-text-dim">
                {title.sources.length}×
              </span>
            )}
          </div>

          {/* Status Badge */}
          {title.status === 'airing' && (
            <div className="absolute top-2 right-2">
              <span className="px-2 py-0.5 rounded-full bg-success/20 text-success text-[10px] font-medium uppercase tracking-wider">
                Airing
              </span>
            </div>
          )}

          {/* HDR Badge */}
          <div className="absolute bottom-2 right-2">
            {title.type === 'movie' && (
              <span className="px-1.5 py-0.5 rounded bg-black/60 text-[9px] font-bold text-white uppercase tracking-wider">
                {title.type}
              </span>
            )}
          </div>

          {/* Library Indicator */}
          {inLib && (
            <div className="absolute top-2 right-2">
              <div className="w-5 h-5 rounded-full bg-success flex items-center justify-center">
                <Check className="w-3 h-3 text-white" />
              </div>
            </div>
          )}
        </div>
      </Link>

      {/* Meta */}
      {showMeta && (
        <div className="mt-2 space-y-0.5 px-0.5">
          <h3 className="text-sm font-medium text-text truncate leading-tight">{label}</h3>
          <div className="flex items-center gap-2 text-xs text-text-dim">
            {title.year && <span>{title.year}</span>}
            {title.episodeCount && <span>{title.episodeCount} eps</span>}
            {title.genres[0] && <span className="truncate">{title.genres[0]}</span>}
          </div>
        </div>
      )}
    </div>
  )
})

export function CardSkeleton({ size = 'md', shape = 'poster' }: { size?: 'sm' | 'md' | 'lg' | 'xl'; shape?: 'poster' | 'wide' | 'square' }) {
  return (
    <div className={cn("flex-shrink-0", SIZE_CLASSES[size])}>
      <div className={cn("rounded-2xl bg-surface overflow-hidden", ASPECT_CLASSES[shape])}>
        <div className="w-full h-full fuse-shimmer" />
      </div>
      <div className="mt-2 space-y-1.5 px-0.5">
        <div className="h-4 w-3/4 rounded bg-surface" />
        <div className="h-3 w-1/2 rounded bg-surface" />
      </div>
    </div>
  )
}
