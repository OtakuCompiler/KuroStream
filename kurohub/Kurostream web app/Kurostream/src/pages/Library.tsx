import { useMemo } from 'react'
import { MediaRow } from '@/components/MediaRow'
import { useStore } from '@/lib/store'

export default function Library() {
  const { library, watchHistory } = useStore()

  const watched = useMemo(() => {
    return watchHistory.filter(h => h.completed).map(h => h.titleId)
  }, [watchHistory])

  return (
    <div className="space-y-8 pb-12 px-4 sm:px-8 pt-6">
      <h1 className="text-2xl font-bold">Your Library</h1>
      {library.length === 0 ? (
        <div className="text-center py-20 text-text-dim">
          Your library is empty. Add titles from browse or search.
        </div>
      ) : (
        <MediaRow heading={`Saved (${library.length})`} items={[]} loading={false} />
      )}
    </div>
  )
}
