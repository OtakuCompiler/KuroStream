import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { Play, Plus, Check, Star, Calendar, Clock, Users, Building2, Film } from 'lucide-react'
import { cn } from '@/lib/utils'
import { useStore } from '@/lib/store'
import { getTitleById, getEpisodes } from '@/lib/metadata/engine'
import { MediaRow } from '@/components/MediaRow'
import type { Title, Episode } from '@/lib/types'

export default function TitleDetail() {
  const { id } = useParams()
  const [title, setTitle] = useState<Title | null>(null)
  const [episodes, setEpisodes] = useState<Episode[]>([])
  const [loading, setLoading] = useState(true)
  const { isInLibrary, addToLibrary, removeFromLibrary } = useStore()

  useEffect(() => {
    if (!id) return
    setLoading(true)
    Promise.all([
      getTitleById(id),
      getEpisodes(id),
    ]).then(([t, eps]) => {
      setTitle(t)
      setEpisodes(eps)
      setLoading(false)
    })
  }, [id])

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="w-8 h-8 border-2 border-primary/30 border-t-primary rounded-full animate-spin" />
      </div>
    )
  }

  if (!title) {
    return (
      <div className="min-h-screen flex items-center justify-center text-text-dim">
        Title not found
      </div>
    )
  }

  const inLib = isInLibrary(title.id)
  const label = title.english || title.title

  return (
    <div className="pb-12">
      <div className="relative h-[40vh] sm:h-[50vh]">
        {title.banner && (
          <img src={title.banner} alt="" className="w-full h-full object-cover" />
        )}
        <div className="absolute inset-0 bg-gradient-to-t from-background via-background/60 to-transparent" />
        <div className="absolute inset-0 bg-gradient-to-r from-background/80 to-transparent" />
      </div>

      <div className="relative -mt-32 px-4 sm:px-8 space-y-6">
        <div className="flex gap-6">
          <div className="hidden sm:block w-48 flex-shrink-0">
            <div className="aspect-[2/3] rounded-2xl overflow-hidden shadow-fuse bg-surface">
              {title.poster ? (
                <img src={title.poster} alt="" className="w-full h-full object-cover" />
              ) : (
                <div className="w-full h-full fuse-shimmer" />
              )}
            </div>
          </div>

          <div className="flex-1 space-y-4 pt-4">
            <div className="flex flex-wrap items-center gap-2">
              {title.score && (
                <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full fuse-glass text-sm font-semibold">
                  <Star className="w-4 h-4 fill-gold text-gold" />
                  {title.score.toFixed(1)}
                </span>
              )}
              <span className="px-3 py-1 rounded-full fuse-glass text-sm text-text-muted">
                {title.type === 'movie' ? 'Movie' : title.type === 'anime' ? 'Anime' : 'TV Series'}
              </span>
              {title.year && (
                <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full fuse-glass text-sm text-text-muted">
                  <Calendar className="w-3.5 h-3.5" />
                  {title.year}
                </span>
              )}
              {title.status === 'airing' && (
                <span className="px-3 py-1 rounded-full bg-success/15 text-success text-sm font-medium">
                  Airing
                </span>
              )}
            </div>

            <h1 className="text-3xl sm:text-5xl font-bold text-glow">{label}</h1>

            <div className="flex flex-wrap gap-2">
              {title.genres.map(g => (
                <span key={g} className="px-2.5 py-1 rounded-lg bg-white/5 text-xs text-text-muted">
                  {g}
                </span>
              ))}
            </div>

            <p className="max-w-2xl text-text-muted leading-relaxed">{title.overview}</p>

            <div className="flex flex-wrap gap-3 pt-2">
              <Link
                to={`/watch/${title.id}`}
                className="inline-flex items-center gap-2 px-6 py-3 rounded-xl bg-primary text-white font-semibold hover:bg-primary/90 transition-all shadow-glow"
              >
                <Play className="w-4 h-4 fill-white" />
                Play
              </Link>
              <button
                onClick={() => inLib ? removeFromLibrary(title.id) : addToLibrary(title.id)}
                className={cn(
                  "inline-flex items-center gap-2 px-5 py-3 rounded-xl font-semibold transition-all",
                  inLib ? "bg-success/15 text-success border border-success/20" : "bg-white/10 text-white hover:bg-white/15"
                )}
              >
                {inLib ? <Check className="w-4 h-4" /> : <Plus className="w-4 h-4" />}
                {inLib ? 'In Library' : 'Add to Library'}
              </button>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 pt-4">
              {title.studios.length > 0 && (
                <div className="space-y-1">
                  <div className="flex items-center gap-1.5 text-xs text-text-dim">
                    <Building2 className="w-3 h-3" />
                    Studios
                  </div>
                  <p className="text-sm text-text">{title.studios.slice(0, 3).join(', ')}</p>
                </div>
              )}
              {title.cast.length > 0 && (
                <div className="space-y-1">
                  <div className="flex items-center gap-1.5 text-xs text-text-dim">
                    <Users className="w-3 h-3" />
                    Cast
                  </div>
                  <p className="text-sm text-text">{title.cast.slice(0, 3).join(', ')}</p>
                </div>
              )}
              {title.episodeCount && (
                <div className="space-y-1">
                  <div className="flex items-center gap-1.5 text-xs text-text-dim">
                    <Film className="w-3 h-3" />
                    Episodes
                  </div>
                  <p className="text-sm text-text">{title.episodeCount}</p>
                </div>
              )}
              {title.duration && (
                <div className="space-y-1">
                  <div className="flex items-center gap-1.5 text-xs text-text-dim">
                    <Clock className="w-3 h-3" />
                    Duration
                  </div>
                  <p className="text-sm text-text">{Math.floor(title.duration / 60)}m</p>
                </div>
              )}
            </div>
          </div>
        </div>

        {episodes.length > 0 && (
          <div className="space-y-4">
            <h2 className="text-xl font-semibold">Episodes</h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
              {episodes.map(ep => (
                <Link
                  key={ep.id}
                  to={`/watch/${title.id}?episode=${ep.number}`}
                  className="flex items-center gap-3 p-3 rounded-xl bg-surface hover:bg-surface-hover transition-colors border border-border group"
                >
                  <div className="w-16 h-10 rounded-lg bg-surface-elevated flex items-center justify-center flex-shrink-0 overflow-hidden">
                    {ep.poster ? (
                      <img src={ep.poster} alt="" className="w-full h-full object-cover" />
                    ) : (
                      <span className="text-xs font-bold text-text-dim">{ep.number}</span>
                    )}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium truncate">{ep.title}</p>
                    <p className="text-xs text-text-dim">Episode {ep.number}</p>
                  </div>
                  <Play className="w-4 h-4 text-text-dim group-hover:text-primary transition-colors" />
                </Link>
              ))}
            </div>
          </div>
        )}

        {title.recommendations && title.recommendations.length > 0 && (
          <MediaRow heading="You May Also Like" items={[]} loading={true} />
        )}
      </div>
    </div>
  )
}
