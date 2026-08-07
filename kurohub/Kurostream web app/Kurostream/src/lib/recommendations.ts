import type { Title, WatchHistory } from './types'
import { useStore } from './store'

interface ScoredTitle {
  title: Title
  score: number
  reasons: string[]
}

export function generateRecommendations(watchHistory: WatchHistory[], library: string[], allTitles: Title[]): Title[] {
  if (watchHistory.length === 0 && library.length === 0) {
    // Cold start: return trending
    return allTitles.slice(0, 20)
  }

  const scored: ScoredTitle[] = []
  const watchedIds = new Set(watchHistory.map(h => h.titleId))
  const libIds = new Set(library)

  // Build user profile
  const genreWeights: Record<string, number> = {}
  const studioWeights: Record<string, number> = {}
  const actorWeights: Record<string, number> = {}
  const timePatterns: number[] = []

  for (const entry of watchHistory) {
    const title = allTitles.find(t => t.id === entry.titleId)
    if (!title) continue

    const weight = entry.completed ? 2.0 : entry.progress > 0.5 ? 1.5 : 1.0

    title.genres.forEach(g => { genreWeights[g] = (genreWeights[g] || 0) + weight })
    title.studios.forEach(s => { studioWeights[s] = (studioWeights[s] || 0) + weight })
    title.cast.forEach(a => { actorWeights[a] = (actorWeights[a] || 0) + weight * 0.5 })
    timePatterns.push(new Date(entry.timestamp).getHours())
  }

  // Normalize weights
  const maxGenre = Math.max(...Object.values(genreWeights), 1)
  const maxStudio = Math.max(...Object.values(studioWeights), 1)
  const maxActor = Math.max(...Object.values(actorWeights), 1)

  Object.keys(genreWeights).forEach(k => genreWeights[k] /= maxGenre)
  Object.keys(studioWeights).forEach(k => studioWeights[k] /= maxStudio)
  Object.keys(actorWeights).forEach(k => actorWeights[k] /= maxActor)

  // Score each candidate
  for (const title of allTitles) {
    if (watchedIds.has(title.id) && !libIds.has(title.id)) continue

    let score = 0
    const reasons: string[] = []

    // Genre match
    title.genres.forEach(g => {
      if (genreWeights[g]) {
        score += genreWeights[g] * 10
        if (!reasons.includes('Similar genres')) reasons.push('Similar genres')
      }
    })

    // Studio match
    title.studios.forEach(s => {
      if (studioWeights[s]) {
        score += studioWeights[s] * 8
        if (!reasons.includes('Same studio')) reasons.push('Same studio')
      }
    })

    // Cast match
    title.cast.forEach(a => {
      if (actorWeights[a]) {
        score += actorWeights[a] * 5
        if (!reasons.includes('Same cast')) reasons.push('Same cast')
      }
    })

    // Score boost
    if (title.score) score += title.score * 2

    // Library boost
    if (libIds.has(title.id)) score += 15

    // Recency boost
    if (title.status === 'airing') score += 5

    // Type preference
    const watchedTypes = watchHistory.map(h => allTitles.find(t => t.id === h.titleId)?.type).filter(Boolean)
    const typePref = watchedTypes.reduce((acc: Record<string, number>, t) => { acc[t!] = (acc[t!] || 0) + 1; return acc }, {})
    if (typePref[title.type]) score += 3

    if (score > 0) {
      scored.push({ title, score, reasons })
    }
  }

  return scored.sort((a, b) => b.score - a.score).slice(0, 30).map(s => s.title)
}

export function getContinueWatching(watchHistory: WatchHistory[]): { titleId: string; episodeId?: string; progress: number; remaining: number }[] {
  const incomplete = watchHistory.filter(h => !h.completed && h.progress > 0.05 && h.progress < 0.95)
  return incomplete
    .sort((a, b) => b.timestamp - a.timestamp)
    .slice(0, 10)
    .map(h => ({
      titleId: h.titleId,
      episodeId: h.episodeId,
      progress: h.progress,
      remaining: Math.max(0, h.duration - h.progress * h.duration),
    }))
}
