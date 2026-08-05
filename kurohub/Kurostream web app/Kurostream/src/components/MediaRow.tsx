import { useRef } from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { cn } from '@/lib/utils'
import { useStore } from '@/lib/store'
import { MediaCard, CardSkeleton } from './MediaCard'
import type { Title } from '@/lib/types'

interface MediaRowProps {
  heading: string
  subheading?: string
  items: Title[] | undefined
  loading?: boolean
  size?: 'sm' | 'md' | 'lg' | 'xl'
  shape?: 'poster' | 'wide' | 'square'
  showMeta?: boolean
}

export function MediaRow({ heading, subheading, items, loading, size = 'md', shape, showMeta = true }: MediaRowProps) {
  const scroller = useRef<HTMLDivElement>(null)
  const { preferences } = useStore()

  const nudge = (dir: 1 | -1) => {
    const el = scroller.current
    if (!el) return
    el.scrollBy({
      left: dir * Math.round(el.clientWidth * 0.8),
      behavior: preferences.reduceMotion ? 'auto' : 'smooth',
    })
  }

  if (!loading && (!items || items.length === 0)) return null

  return (
    <section className="space-y-3">
      <header className="flex items-end justify-between gap-4 px-4 sm:px-8">
        <div>
          <h2 className="text-lg sm:text-xl font-semibold text-text">{heading}</h2>
          {subheading && <p className="text-sm text-text-dim mt-0.5">{subheading}</p>}
        </div>
        {items && items.length > 4 && (
          <div className="flex items-center gap-1">
            <button onClick={() => nudge(-1)} className="p-1.5 rounded-lg hover:bg-white/5 transition-colors text-text-dim">
              <ChevronLeft className="w-4 h-4" />
            </button>
            <button onClick={() => nudge(1)} className="p-1.5 rounded-lg hover:bg-white/5 transition-colors text-text-dim">
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        )}
      </header>

      <div 
        ref={scroller}
        className={cn(
          "flex gap-3 sm:gap-4 overflow-x-auto px-4 sm:px-8 pb-2 arctic-scrollbar-hide snap-x snap-mandatory",
          preferences.reduceMotion && "scroll-smooth"
        )}
      >
        {loading ? (
          Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="snap-start">
              <CardSkeleton size={size} shape={shape || preferences.cardShape} />
            </div>
          ))
        ) : (
          items?.map((item, i) => (
            <div key={item.id} className="snap-start">
              <MediaCard 
                title={item} 
                priority={i < 4} 
                size={size} 
                shape={shape || preferences.cardShape}
                showMeta={showMeta}
              />
            </div>
          ))
        )}
      </div>
    </section>
  )
}
