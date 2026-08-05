import { useEffect, useState } from 'react'
import { Hero } from '@/components/Hero'
import { MediaRow } from '@/components/MediaRow'
import { getTrending, getPopular, getSeasonalAnime, getUpcoming } from '@/lib/metadata/engine'
import { generateRecommendations, getContinueWatching } from '@/lib/recommendations'
import { useStore } from '@/lib/store'
import type { Title } from '@/lib/types'

export default function Home() {
  const [trending, setTrending] = useState<Title[]>([])
  const [popular, setPopular] = useState<Title[]>([])
  const [seasonal, setSeasonal] = useState<Title[]>([])
  const [upcoming, setUpcoming] = useState<Title[]>([])
  const [recommended, setRecommended] = useState<Title[]>([])
  const [continueWatching, setContinueWatching] = useState<any[]>([])
  const [loading, setLoading] = useState(true)
  const { watchHistory, library } = useStore()

  useEffect(() => {
    const load = async () => {
      setLoading(true)
      const [t, p, s, u] = await Promise.all([
        getTrending('anime'),
        getPopular('anime'),
        getSeasonalAnime(),
        getUpcoming('anime'),
      ])
      setTrending(t)
      setPopular(p)
      setSeasonal(s)
      setUpcoming(u)

      const allTitles = [...t, ...p, ...s, ...u]
      const recs = generateRecommendations(watchHistory, library, allTitles)
      setRecommended(recs)

      const cont = getContinueWatching(watchHistory)
      setContinueWatching(cont)

      setLoading(false)
    }
    load()
  }, [watchHistory, library])

  return (
    <div className="space-y-8 pb-12">
      <Hero title={trending[0]} loading={loading} />

      {continueWatching.length > 0 && (
        <MediaRow heading="Continue Watching" subheading="Resume where you left off" items={[]} loading={loading} />
      )}

      <MediaRow heading="Trending Now" subheading="Most popular this week" items={trending} loading={loading} />
      <MediaRow heading="This Season" subheading="Currently airing" items={seasonal} loading={loading} />
      <MediaRow heading="Recommended For You" subheading="Based on your watch history" items={recommended} loading={loading} />
      <MediaRow heading="Popular" subheading="All-time favorites" items={popular} loading={loading} />
      <MediaRow heading="Coming Soon" subheading="Upcoming releases" items={upcoming} loading={loading} />
    </div>
  )
}
