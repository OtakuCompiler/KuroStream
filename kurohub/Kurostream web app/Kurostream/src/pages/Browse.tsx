import { useState, useEffect } from 'react'
import { MediaRow } from '@/components/MediaRow'
import { getTrending, getPopular, getSeasonalAnime } from '@/lib/metadata/engine'
import type { Title } from '@/lib/types'

const CATEGORIES = [
  { id: 'anime', label: 'Anime' },
  { id: 'movie', label: 'Movies' },
  { id: 'series', label: 'TV Series' },
]

export default function Browse() {
  const [activeCategory, setActiveCategory] = useState('anime')
  const [items, setItems] = useState<Title[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    getTrending(activeCategory as any).then(data => {
      setItems(data)
      setLoading(false)
    })
  }, [activeCategory])

  return (
    <div className="space-y-6 pb-12 px-4 sm:px-8 pt-4">
      <div className="flex items-center gap-2 overflow-x-auto pb-2 arctic-scrollbar-hide">
        {CATEGORIES.map(cat => (
          <button
            key={cat.id}
            onClick={() => setActiveCategory(cat.id)}
            className={`px-4 py-2 rounded-xl text-sm font-medium transition-all ${
              activeCategory === cat.id
                ? 'bg-primary/15 text-primary'
                : 'bg-white/5 text-text-muted hover:bg-white/10'
            }`}
          >
            {cat.label}
          </button>
        ))}
      </div>
      <MediaRow heading={`${CATEGORIES.find(c => c.id === activeCategory)?.label}`} items={items} loading={loading} size="lg" />
    </div>
  )
}
