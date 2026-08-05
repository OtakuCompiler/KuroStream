import type { IntroSkipData } from '../types'

// ─── AniSkip API ───
export async function fetchSkipTimes(malId: number, episode: number): Promise<IntroSkipData | null> {
  try {
    const res = await fetch(`https://api.aniskip.com/v2/skip-times/${malId}/${episode}?types=ed&types=op&types=recap&episodeLength=0`)
    if (!res.ok) return null
    const data = await res.json()

    let introStart = 0, introEnd = 0, outroStart = 0, outroEnd = 0

    for (const result of data.results || []) {
      if (result.skipType === 'op') {
        introStart = result.interval.startTime
        introEnd = result.interval.endTime
      }
      if (result.skipType === 'ed') {
        outroStart = result.interval.startTime
        outroEnd = result.interval.endTime
      }
    }

    return { introStart, introEnd, outroStart, outroEnd, confidence: 0.95 }
  } catch { return null }
}

// ─── Local Skip Detection (heuristic) ───
export function detectIntroHeuristic(video: HTMLVideoElement): Promise<IntroSkipData | null> {
  return new Promise((resolve) => {
    // Use canvas to analyze frames for intro detection
    // This is a simplified version - real implementation would use ML
    const canvas = document.createElement('canvas')
    const ctx = canvas.getContext('2d')
    if (!ctx) { resolve(null); return }

    canvas.width = 64
    canvas.height = 36

    const samples: ImageData[] = []
    const duration = video.duration
    if (!duration || duration < 120) { resolve(null); return }

    // Sample frames at 30s, 60s, 90s for intro pattern
    const times = [30, 60, 90]
    let loaded = 0

    times.forEach(t => {
      video.currentTime = Math.min(t, duration - 5)
      video.addEventListener('seeked', function onSeeked() {
        video.removeEventListener('seeked', onSeeked)
        ctx.drawImage(video, 0, 0, 64, 36)
        samples.push(ctx.getImageData(0, 0, 64, 36))
        loaded++
        if (loaded === times.length) {
          // Simple brightness comparison heuristic
          const brightnesses = samples.map(s => {
            let sum = 0
            for (let i = 0; i < s.data.length; i += 4) {
              sum += (s.data[i] + s.data[i+1] + s.data[i+2]) / 3
            }
            return sum / (s.data.length / 4)
          })

          // If brightness is consistent across samples, likely intro
          const avg = brightnesses.reduce((a, b) => a + b, 0) / brightnesses.length
          const variance = brightnesses.reduce((a, b) => a + Math.pow(b - avg, 2), 0) / brightnesses.length

          if (variance < 500) {
            resolve({ introStart: 0, introEnd: 90, outroStart: duration - 90, outroEnd: duration, confidence: 0.3 })
          } else {
            resolve(null)
          }
        }
      }, { once: true })
    })

    setTimeout(() => resolve(null), 5000)
  })
}

// ─── Chapter Detection ───
export function detectChapters(video: HTMLVideoElement): { time: number; label: string }[] {
  const chapters: { time: number; label: string }[] = []
  const duration = video.duration
  if (!duration) return chapters

  // Standard chapter markers
  if (duration > 300) {
    chapters.push({ time: 0, label: 'Start' })
    if (duration > 600) chapters.push({ time: duration * 0.25, label: '25%' })
    if (duration > 900) chapters.push({ time: duration * 0.5, label: '50%' })
    if (duration > 600) chapters.push({ time: duration * 0.75, label: '75%' })
    chapters.push({ time: duration - 1, label: 'End' })
  }

  return chapters
}

// ─── Bookmark System ───
const BOOKMARKS_KEY = 'arctic-bookmarks'

export interface Bookmark {
  id: string
  titleId: string
  episodeId?: string
  time: number
  label: string
  createdAt: number
}

export function getBookmarks(titleId: string, episodeId?: string): Bookmark[] {
  try {
    const all = JSON.parse(localStorage.getItem(BOOKMARKS_KEY) || '[]')
    return all.filter((b: Bookmark) => b.titleId === titleId && b.episodeId === episodeId)
  } catch { return [] }
}

export function addBookmark(bookmark: Bookmark): void {
  try {
    const all = JSON.parse(localStorage.getItem(BOOKMARKS_KEY) || '[]')
    all.push(bookmark)
    localStorage.setItem(BOOKMARKS_KEY, JSON.stringify(all.slice(-100)))
  } catch {}
}

export function removeBookmark(id: string): void {
  try {
    const all = JSON.parse(localStorage.getItem(BOOKMARKS_KEY) || '[]')
    localStorage.setItem(BOOKMARKS_KEY, JSON.stringify(all.filter((b: Bookmark) => b.id !== id)))
  } catch {}
}
