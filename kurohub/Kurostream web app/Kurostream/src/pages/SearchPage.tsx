import { GlobalSearch } from '@/components/Search'
import { MediaRow } from '@/components/MediaRow'
import { useStore } from '@/lib/store'

export default function SearchPage() {
  const { searchResults, searchLoading } = useStore()

  return (
    <div className="space-y-8 pb-12 px-4 sm:px-8 pt-6">
      <GlobalSearch />
      {searchResults.length > 0 && (
        <MediaRow heading="Search Results" items={searchResults} loading={searchLoading} size="lg" />
      )}
    </div>
  )
}
