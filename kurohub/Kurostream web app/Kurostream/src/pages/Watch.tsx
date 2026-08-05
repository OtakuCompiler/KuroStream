import { useEffect, useState } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import ArcticPlayer from '@/components/player/ArcticPlayer'
import { getTitleById, getEpisodes } from '@/lib/metadata/engine'
import { searchTorrents } from '@/lib/p2p/engine'
import type { Title, Episode, Source } from '@/lib/types'

export default function Watch() {
  const { id } = useParams()
  const [searchParams] = useSearchParams()
  const [title, setTitle] = useState<Title | null>(null)
  const [episode, setEpisode] = useState<Episode | undefined>()
  const [source, setSource] = useState<Source | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!id) return
    const load = async () => {
      setLoading(true)
      const t = await getTitleById(id)
      setTitle(t)

      const epNum = parseInt(searchParams.get('episode') || '1')
      if (t) {
        const eps = await getEpisodes(id)
        const ep = eps.find(e => e.number === epNum)
        setEpisode(ep)

        const torrents = await searchTorrents(t.title)
        if (torrents.length > 0) {
          setSource(torrents[0])
        } else {
          setSource({
            id: 'demo',
            url: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4',
            quality: '1080p',
            provider: 'Demo',
            type: 'direct',
            audioLang: ['en'],
            subtitleLang: ['en'],
            score: 50,
          })
        }
      }
      setLoading(false)
    }
    load()
  }, [id, searchParams])

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="w-10 h-10 border-2 border-primary/30 border-t-primary rounded-full animate-spin" />
      </div>
    )
  }

  if (!title || !source) {
    return (
      <div className="min-h-screen flex items-center justify-center text-text-dim">
        Unable to load video
      </div>
    )
  }

  return (
    <div className="fixed inset-0 bg-black z-50">
      <ArcticPlayer title={title} episode={episode} source={source} />
    </div>
  )
}
