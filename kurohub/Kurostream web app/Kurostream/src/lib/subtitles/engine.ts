import type { SubtitleTrack } from '../types'

export async function searchOpenSubtitles(query: string, lang: string = 'en'): Promise<SubtitleTrack[]> {
  try {
    const res = await fetch(`https://api.opensubtitles.com/api/v1/subtitles?query=${encodeURIComponent(query)}&languages=${lang}&order_by=download_count`, {
      headers: { 'Api-Key': 'demo', 'Content-Type': 'application/json' }
    })
    if (!res.ok) return []
    const data = await res.json()
    return (data.data || []).slice(0, 10).map((s: any) => ({
      id: `os-${s.id}`,
      label: s.attributes?.release || s.attributes?.filename || 'Unknown',
      language: s.attributes?.language || lang,
      url: s.attributes?.url || '',
      format: (s.attributes?.format || 'srt').toLowerCase() as 'srt' | 'ass' | 'vtt',
      isDefault: false,
      isSDH: s.attributes?.hearing_impaired || false,
      isForced: s.attributes?.foreign_parts_only || false,
    }))
  } catch { return [] }
}

export async function searchSubDL(imdbId: string, tmdbId?: number, season?: number, episode?: number): Promise<SubtitleTrack[]> {
  try {
    const params = new URLSearchParams({ api_key: 'demo' })
    if (imdbId) params.set('imdb_id', imdbId)
    if (tmdbId) params.set('tmdb_id', String(tmdbId))
    if (season) params.set('seasons', String(season))
    if (episode) params.set('episodes', String(episode))

    const res = await fetch(`https://api.subdl.com/api/v1/subtitles?${params.toString()}`)
    if (!res.ok) return []
    const data = await res.json()
    return (data.subtitles || []).map((s: any, i: number) => ({
      id: `subdl-${i}`,
      label: s.release_name || s.language || 'Unknown',
      language: s.language || 'en',
      url: s.url || s.path || '',
      format: (s.format || 'srt').toLowerCase() as 'srt' | 'ass' | 'vtt',
      isDefault: s.default || false,
      isSDH: false,
      isForced: false,
    }))
  } catch { return [] }
}

export async function searchAddic7ed(query: string, season?: number, episode?: number): Promise<SubtitleTrack[]> {
  try {
    const res = await fetch(`https://corsproxy.io/?https://www.addic7ed.com/search.php?search=${encodeURIComponent(query)}`)
    if (!res.ok) return []
    return []
  } catch { return [] }
}

export async function searchPodnapisi(query: string, lang: string = 'en'): Promise<SubtitleTrack[]> {
  try {
    const res = await fetch(`https://www.podnapisi.net/subtitles/search/?keywords=${encodeURIComponent(query)}&language=${lang}`)
    if (!res.ok) return []
    return []
  } catch { return [] }
}

export async function searchSubtitles(title: string, options?: { imdbId?: string; tmdbId?: number; season?: number; episode?: number; lang?: string }): Promise<SubtitleTrack[]> {
  const [os, subdl] = await Promise.all([
    searchOpenSubtitles(title, options?.lang || 'en'),
    searchSubDL(options?.imdbId || '', options?.tmdbId, options?.season, options?.episode),
  ])

  const all = [...os, ...subdl]
  const seen = new Set<string>()
  return all.filter(s => {
    const key = `${s.language}-${s.label}`
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}

export function parseSRT(content: string): { start: number; end: number; text: string }[] {
  const blocks = content.trim().split(/\n\s*\n/)
  const cues: { start: number; end: number; text: string }[] = []

  for (const block of blocks) {
    const lines = block.trim().split('\n')
    if (lines.length < 3) continue
    const timeLine = lines[1]
    const match = timeLine.match(/(\d{2}:\d{2}:\d{2}[,\.]\d{3})\s*-->\s*(\d{2}:\d{2}:\d{2}[,\.]\d{3})/)
    if (!match) continue
    const text = lines.slice(2).join('\n').replace(/<[^>]+>/g, '')
    cues.push({ start: parseTime(match[1]), end: parseTime(match[2]), text })
  }

  return cues
}

export function parseASS(content: string): { start: number; end: number; text: string; style?: string }[] {
  const lines = content.split('\n')
  const cues: { start: number; end: number; text: string; style?: string }[] = []
  let inEvents = false

  for (const line of lines) {
    if (line.startsWith('[Events]')) { inEvents = true; continue }
    if (line.startsWith('[') && inEvents) { inEvents = false; continue }
    if (!inEvents || !line.startsWith('Dialogue:')) continue

    const parts = line.substring(9).split(',')
    if (parts.length < 10) continue
    const start = parseTime(parts[1].trim())
    const end = parseTime(parts[2].trim())
    const text = parts.slice(9).join(',').replace(/\\N/g, '\n').replace(/\{[^}]*\}/g, '')
    cues.push({ start, end, text, style: parts[3].trim() })
  }

  return cues
}

export function parseVTT(content: string): { start: number; end: number; text: string }[] {
  const lines = content.trim().split('\n')
  const cues: { start: number; end: number; text: string }[] = []
  let i = 0

  while (i < lines.length) {
    if (lines[i].includes('-->')) {
      const match = lines[i].match(/(\d{2}:\d{2}:\d{2}\.\d{3})\s*-->\s*(\d{2}:\d{2}:\d{2}\.\d{3})/)
      if (match) {
        const textLines: string[] = []
        i++
        while (i < lines.length && lines[i].trim() !== '' && !lines[i].includes('-->')) {
          textLines.push(lines[i].replace(/<[^>]+>/g, ''))
          i++
        }
        cues.push({ start: parseTime(match[1]), end: parseTime(match[2]), text: textLines.join('\n') })
        continue
      }
    }
    i++
  }

  return cues
}

function parseTime(t: string): number {
  const parts = t.replace(',', '.').split(':')
  return parseFloat(parts[0]) * 3600 + parseFloat(parts[1]) * 60 + parseFloat(parts[2])
}

export function syncSubtitles(cues: { start: number; end: number; text: string }[], offsetMs: number): { start: number; end: number; text: string }[] {
  const offset = offsetMs / 1000
  return cues.map(c => ({ ...c, start: Math.max(0, c.start + offset), end: Math.max(0, c.end + offset) }))
}

export function mergeSubtitles(primary: { start: number; end: number; text: string }[], secondary: { start: number; end: number; text: string }[]): { start: number; end: number; text: string; secondary?: string }[] {
  const merged: { start: number; end: number; text: string; secondary?: string }[] = []
  for (const p of primary) {
    const match = secondary.find(s => Math.abs(s.start - p.start) < 1)
    merged.push({ ...p, secondary: match?.text })
  }
  return merged
}

export function detectSubtitleLang(text: string): string {
  const samples: Record<string, RegExp> = {
    ja: /[\u3040-\u309F\u30A0-\u30FF]/,
    ko: /[\uAC00-\uD7AF]/,
    zh: /[\u4E00-\u9FFF]/,
    ar: /[\u0600-\u06FF]/,
    ru: /[\u0400-\u04FF]/,
    hi: /[\u0900-\u097F]/,
  }

  for (const [lang, regex] of Object.entries(samples)) {
    if (regex.test(text)) return lang
  }
  return 'en'
}
