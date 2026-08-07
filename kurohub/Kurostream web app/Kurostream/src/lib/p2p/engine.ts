import type { Source } from '../types'

interface TorrentFile {
  name: string
  length: number
  progress: number
  done: boolean
  getBlobURL(cb: (err: any, url: string) => void): void
}

interface Torrent {
  infoHash: string
  magnetURI: string
  files: TorrentFile[]
  downloadSpeed: number
  uploadSpeed: number
  progress: number
  ratio: number
  numPeers: number
  timeRemaining: number
  done: boolean
  destroy(): void
  on(event: string, fn: (...args: any[]) => void): void
}

declare const WebTorrent: any

let client: any = null
let activeTorrents: Map<string, Torrent> = new Map()

function getClient() {
  if (!client && typeof window !== 'undefined') {
    try {
      client = new WebTorrent({
        tracker: {
          wrtc: false,
          announce: [
            'wss://tracker.openwebtorrent.com',
            'wss://tracker.files.fm:7073/announce',
            'wss://spacetradersapi-chatbox.herokuapp.com:443/announce',
            'udp://tracker.opentrackr.org:1337/announce',
            'udp://tracker.openbittorrent.com:6969/announce',
            'udp://tracker.leechers-paradise.org:6969/announce',
          ]
        },
        dht: true,
        webSeeds: true,
        maxConns: 200,
      })
    } catch (e) {
      console.warn('[Arctic] WebTorrent init failed:', e)
    }
  }
  return client
}

export async function addTorrent(magnet: string, opts?: { maxFileSize?: number }): Promise<{ torrent: Torrent; streamUrl: string }> {
  const c = getClient()
  if (!c) throw new Error('WebTorrent not available')

  return new Promise((resolve, reject) => {
    const existing = activeTorrents.get(magnet)
    if (existing) {
      const file = existing.files.find((f: any) => f.length > 10 * 1024 * 1024 && /\.(mp4|mkv|avi|mov|webm)$/i.test(f.name))
      if (file) {
        const streamUrl = URL.createObjectURL(new Blob([]))
        resolve({ torrent: existing, streamUrl })
        return
      }
    }

    const torrent = c.add(magnet, { announce: c.tracker.announce }, (t: Torrent) => {
      const videoFile = t.files.find((f: any) => f.length > 10 * 1024 * 1024 && /\.(mp4|mkv|avi|mov|webm)$/i.test(f.name))
      if (!videoFile) { t.destroy(); reject(new Error('No video file found')); return }

      videoFile.getBlobURL((err: any, url: string) => {
        if (err) { reject(err); return }
        activeTorrents.set(magnet, t)
        resolve({ torrent: t, streamUrl: url })
      })
    })

    torrent.on('error', (err: any) => reject(err))
    setTimeout(() => reject(new Error('Torrent add timeout')), 60000)
  })
}

export function removeTorrent(magnet: string) {
  const t = activeTorrents.get(magnet)
  if (t) { t.destroy(); activeTorrents.delete(magnet) }
}

export function getTorrentStats(magnet: string) {
  const t = activeTorrents.get(magnet)
  if (!t) return null
  return {
    progress: t.progress,
    downloadSpeed: t.downloadSpeed,
    uploadSpeed: t.uploadSpeed,
    peers: t.numPeers,
    ratio: t.ratio,
    timeRemaining: t.timeRemaining,
    done: t.done,
  }
}

export function destroyAllTorrents() {
  activeTorrents.forEach(t => t.destroy())
  activeTorrents.clear()
  if (client) { client.destroy(); client = null }
}

export async function createP2POffer(): Promise<{ offer: RTCSessionDescriptionInit; peerId: string }> {
  const pc = new RTCPeerConnection({
    iceServers: [
      { urls: 'stun:stun.l.google.com:19302' },
      { urls: 'stun:stun1.l.google.com:19302' },
    ]
  })
  const peerId = Math.random().toString(36).substring(2, 15)

  const offer = await pc.createOffer()
  await pc.setLocalDescription(offer)

  await new Promise<void>(resolve => {
    if (pc.iceGatheringState === 'complete') { resolve(); return }
    pc.onicegatheringstatechange = () => { if (pc.iceGatheringState === 'complete') resolve() }
    setTimeout(resolve, 5000)
  })

  return { offer: pc.localDescription!, peerId }
}

export async function acceptP2POffer(peerId: string, offer: RTCSessionDescriptionInit): Promise<RTCSessionDescriptionInit> {
  const pc = new RTCPeerConnection({
    iceServers: [
      { urls: 'stun:stun.l.google.com:19302' },
      { urls: 'stun:stun1.l.google.com:19302' },
    ]
  })

  await pc.setRemoteDescription(offer)
  const answer = await pc.createAnswer()
  await pc.setLocalDescription(answer)

  await new Promise<void>(resolve => {
    if (pc.iceGatheringState === 'complete') { resolve(); return }
    pc.onicegatheringstatechange = () => { if (pc.iceGatheringState === 'complete') resolve() }
    setTimeout(resolve, 5000)
  })

  return pc.localDescription!
}

export async function searchTorrents(query: string): Promise<Source[]> {
  const sources: Source[] = []

  try {
    const res = await fetch(`https://corsproxy.io/?https://nyaa.si/?page=rss&q=${encodeURIComponent(query)}&c=1_2&f=0`)
    const text = await res.text()
    const parser = new DOMParser()
    const doc = parser.parseFromString(text, 'application/xml')
    const items = doc.querySelectorAll('item')
    items.forEach((item, i) => {
      if (i >= 15) return
      const title = item.querySelector('title')?.textContent || ''
      const link = item.querySelector('link')?.textContent || ''
      const size = item.querySelector('nyaa\\:size, size')?.textContent || ''
      const seeders = parseInt(item.querySelector('nyaa\\:seeders, seeders')?.textContent || '0')

      let quality: Source['quality'] = 'unknown'
      if (/\b(2160p|4K|UHD)\b/i.test(title)) quality = '4K'
      else if (/\b1080p\b/i.test(title)) quality = '1080p'
      else if (/\b720p\b/i.test(title)) quality = '720p'
      else if (/\b480p\b/i.test(title)) quality = '480p'

      sources.push({
        id: `torrent-nyaa-${i}`,
        url: link,
        quality,
        provider: 'Nyaa',
        type: 'torrent',
        size: parseSize(size),
        seeds: seeders,
        peers: Math.floor(seeders * 0.3),
        audioLang: ['ja'],
        subtitleLang: [],
        score: Math.min(100, seeders / 10),
      })
    })
  } catch (e) { console.warn('Nyaa search failed:', e) }

  return sources.sort((a, b) => (b.score || 0) - (a.score || 0))
}

function parseSize(sizeStr: string): number {
  const match = sizeStr.match(/([\d.]+)\s*(GiB|MiB|KiB|GB|MB|KB)/i)
  if (!match) return 0
  const val = parseFloat(match[1])
  const unit = match[2].toUpperCase()
  const mult = unit.startsWith('G') ? 1024 * 1024 * 1024 : unit.startsWith('M') ? 1024 * 1024 : 1024
  return Math.floor(val * mult)
}
