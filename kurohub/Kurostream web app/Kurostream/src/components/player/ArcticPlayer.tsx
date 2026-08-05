import { useRef, useEffect, useState, useCallback, useMemo } from 'react'
import { 
  Play, Pause, SkipForward, SkipBack, Volume2, VolumeX, Maximize, Minimize, 
  Settings, Subtitles, Bookmark, Aperture, ChevronRight, RotateCcw, 
  FastForward, Rewind, PictureInPicture2, Cast, Monitor,
  Gauge, Cpu, Thermometer
} from 'lucide-react'
import Hls from 'hls.js'
import screenfull from 'screenfull'
import { cn, formatDuration, getDeviceProfile } from '@/lib/utils'
import { useStore } from '@/lib/store'
import type { Title, Episode, Source, SubtitleTrack, PlaybackPreset } from '@/lib/types'
import { fetchSkipTimes, detectIntroHeuristic, detectChapters, getBookmarks, addBookmark, removeBookmark } from '@/lib/aniskip'
import { searchSubtitles, parseSRT, parseASS, parseVTT, syncSubtitles } from '@/lib/subtitles/engine'
import { addTorrent, removeTorrent, getTorrentStats } from '@/lib/p2p/engine'

interface ArcticPlayerProps {
  title: Title
  episode?: Episode
  source: Source
  onNext?: () => void
  onPrev?: () => void
  onClose?: () => void
}

export default function ArcticPlayer({ title, episode, source, onNext, onPrev, onClose }: ArcticPlayerProps) {
  const videoRef = useRef<HTMLVideoElement>(null)
  const containerRef = useRef<HTMLDivElement>(null)
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const hlsRef = useRef<Hls | null>(null)
  const controlsTimeoutRef = useRef<ReturnType<typeof setTimeout>>()
  const skipCheckRef = useRef<ReturnType<typeof setInterval>>()
  const statsIntervalRef = useRef<ReturnType<typeof setInterval>>()

  const { preferences, updatePreferences, addWatchHistory } = useStore()
  const [isPlaying, setIsPlaying] = useState(false)
  const [currentTime, setCurrentTime] = useState(0)
  const [duration, setDuration] = useState(0)
  const [volume, setVolume] = useState(1)
  const [isMuted, setIsMuted] = useState(false)
  const [isFullscreen, setIsFullscreen] = useState(false)
  const [showControls, setShowControls] = useState(true)
  const [buffered, setBuffered] = useState(0)
  const [playbackRate, setPlaybackRate] = useState(1)
  const [quality, setQuality] = useState<string>('auto')
  const [subtitles, setSubtitles] = useState<SubtitleTrack[]>([])
  const [activeSubtitle, setActiveSubtitle] = useState<SubtitleTrack | null>(null)
  const [subtitleCues, setSubtitleCues] = useState<{ start: number; end: number; text: string }[]>([])
  const [subtitleOffset, setSubtitleOffset] = useState(0)
  const [showSettings, setShowSettings] = useState(false)
  const [showSubMenu, setShowSubMenu] = useState(false)
  const [skipData, setSkipData] = useState<{ introStart: number; introEnd: number; outroStart: number; outroEnd: number } | null>(null)
  const [showSkipIntro, setShowSkipIntro] = useState(false)
  const [showSkipOutro, setShowSkipOutro] = useState(false)
  const [chapters, setChapters] = useState<{ time: number; label: string }[]>([])
  const [bookmarks, setBookmarks] = useState<any[]>([])
  const [abRepeat, setAbRepeat] = useState<{ a: number | null; b: number | null }>({ a: null, b: null })
  const [isABLooping, setIsABLooping] = useState(false)
  const [frameStepMode, setFrameStepMode] = useState(false)
  const [showStats, setShowStats] = useState(false)
  const [playerStats, setPlayerStats] = useState({ fps: 0, dropped: 0, bitrate: 0, resolution: '', codec: '' })
  const [upscalingEnabled, setUpscalingEnabled] = useState(false)
  const [preset, setPreset] = useState<PlaybackPreset>({ id: 'default', name: 'Default', brightness: 100, contrast: 100, saturation: 100, sharpness: 0, denoise: false, upscaling: 'none' })
  const [torrentProgress, setTorrentProgress] = useState(0)
  const [torrentPeers, setTorrentPeers] = useState(0)
  const [torrentSpeed, setTorrentSpeed] = useState(0)
  const [pipMode, setPipMode] = useState(false)
  const [audioTracks, setAudioTracks] = useState<MediaStreamTrack[]>([])
  const [activeAudioTrack, setActiveAudioTrack] = useState(0)
  const [showChapterMenu, setShowChapterMenu] = useState(false)
  const [showBookmarkMenu, setShowBookmarkMenu] = useState(false)
  const [showABMenu, setShowABMenu] = useState(false)
  const [showPresets, setShowPresets] = useState(false)
  const [presets, setPresets] = useState<PlaybackPreset[]>([
    { id: 'default', name: 'Default', brightness: 100, contrast: 100, saturation: 100, sharpness: 0, denoise: false, upscaling: 'none' },
    { id: 'cinema', name: 'Cinema', brightness: 90, contrast: 110, saturation: 95, sharpness: 20, denoise: true, upscaling: 'lanczos' },
    { id: 'vivid', name: 'Vivid', brightness: 105, contrast: 115, saturation: 130, sharpness: 30, denoise: false, upscaling: 'cas' },
    { id: 'game', name: 'Game Mode', brightness: 110, contrast: 105, saturation: 110, sharpness: 40, denoise: false, upscaling: 'fsr' },
  ])
  const [isBuffering, setIsBuffering] = useState(false)
  const [showOverlay, setShowOverlay] = useState(true)
  const [lastActivity, setLastActivity] = useState(Date.now())
  const [showSpeedMenu, setShowSpeedMenu] = useState(false)
  const [showQualityMenu, setShowQualityMenu] = useState(false)
  const [availableQualities, setAvailableQualities] = useState<string[]>(['auto'])
  const [deviceProfile] = useState(getDeviceProfile())
  const [memoryWarning, setMemoryWarning] = useState(false)

  const video = videoRef.current

  // ─── Initialize Player ───
  useEffect(() => {
    const v = videoRef.current
    if (!v) return

    // Adaptive bitrate based on device
    const profile = getDeviceProfile()
    if (profile === 'LOW') {
      v.setAttribute('preload', 'metadata')
      v.setAttribute('playsinline', 'true')
    }

    // HLS setup
    if (source.type === 'hls' || source.url.includes('.m3u8')) {
      if (Hls.isSupported()) {
        const hls = new Hls({
          maxBufferLength: profile === 'LOW' ? 30 : 60,
          maxMaxBufferLength: profile === 'LOW' ? 60 : 300,
          capLevelToPlayerSize: true,
          startLevel: profile === 'LOW' ? 0 : -1,
          abrEwmaDefaultEstimate: 500000,
          abrBandWidthFactor: 0.95,
          abrBandWidthUpFactor: 0.7,
          fragLoadingTimeOut: 20000,
          manifestLoadingTimeOut: 10000,
          levelLoadingTimeOut: 10000,
        })
        hls.loadSource(source.url)
        hls.attachMedia(v)
        hlsRef.current = hls

        hls.on(Hls.Events.MANIFEST_PARSED, (_, data) => {
          const levels = data.levels.map(l => `${l.height}p`)
          setAvailableQualities(['auto', ...levels])
        })

        hls.on(Hls.Events.LEVEL_SWITCHED, (_, data) => {
          const level = hls.levels[data.level]
          if (level) setQuality(`${level.height}p`)
        })

        hls.on(Hls.Events.ERROR, (_, data) => {
          if (data.fatal) {
            console.error('[Arctic] HLS fatal error:', data.type, data.details)
            if (data.type === Hls.ErrorTypes.NETWORK_ERROR) hls.startLoad()
            else if (data.type === Hls.ErrorTypes.MEDIA_ERROR) hls.recoverMediaError()
          }
        })
      } else if (v.canPlayType('application/vnd.apple.mpegurl')) {
        v.src = source.url
      }
    } else if (source.type === 'torrent' || source.url.startsWith('magnet:')) {
      // Torrent streaming
      addTorrent(source.url).then(({ streamUrl }) => {
        v.src = streamUrl
        // Monitor torrent stats
        const interval = setInterval(() => {
          const stats = getTorrentStats(source.url)
          if (stats) {
            setTorrentProgress(stats.progress * 100)
            setTorrentPeers(stats.peers)
            setTorrentSpeed(stats.downloadSpeed)
          }
        }, 1000)
        return () => clearInterval(interval)
      }).catch(err => {
        console.error('[Arctic] Torrent error:', err)
      })
    } else {
      v.src = source.url
    }

    // Resume playback
    const savedProgress = useStore.getState().getWatchProgress(title.id, episode?.id)
    if (savedProgress > 0.05 && savedProgress < 0.95) {
      v.addEventListener('loadedmetadata', () => {
        v.currentTime = savedProgress * v.duration
      }, { once: true })
    }

    // Fetch skip times
    if (title.ids.mal && episode) {
      fetchSkipTimes(title.ids.mal, episode.number).then(data => {
        if (data) setSkipData(data)
      })
    }

    // Fetch subtitles
    searchSubtitles(title.title, {
      imdbId: title.ids.imdb,
      tmdbId: title.ids.tmdb,
      season: episode?.season,
      episode: episode?.number,
      lang: preferences.defaultSubtitle,
    }).then(subs => setSubtitles(subs))

    // Load bookmarks
    setBookmarks(getBookmarks(title.id, episode?.id))

    // Detect chapters
    v.addEventListener('loadedmetadata', () => {
      setChapters(detectChapters(v))
      setDuration(v.duration)
    }, { once: true })

    // Audio normalization
    try {
      const audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)()
      const sourceNode = audioCtx.createMediaElementSource(v)
      const gainNode = audioCtx.createGain()
      const compressor = audioCtx.createDynamicsCompressor()
      compressor.threshold.value = -24
      compressor.knee.value = 30
      compressor.ratio.value = 12
      compressor.attack.value = 0.003
      compressor.release.value = 0.25
      sourceNode.connect(compressor)
      compressor.connect(gainNode)
      gainNode.connect(audioCtx.destination)
      gainNode.gain.value = 1.0
    } catch {}

    return () => {
      hlsRef.current?.destroy()
      if (source.type === 'torrent' || source.url.startsWith('magnet:')) {
        removeTorrent(source.url)
      }
    }
  }, [source.url, source.type])

  // ─── Playback State Handlers ───
  useEffect(() => {
    const v = videoRef.current
    if (!v) return

    const onTimeUpdate = () => {
      setCurrentTime(v.currentTime)
      setBuffered(v.buffered.length > 0 ? v.buffered.end(v.buffered.length - 1) : 0)

      // Skip intro detection
      if (skipData && preferences.autoSkipIntro) {
        if (v.currentTime >= skipData.introStart && v.currentTime < skipData.introEnd - 5) {
          setShowSkipIntro(true)
        } else {
          setShowSkipIntro(false)
        }
      }

      // Skip outro detection
      if (skipData && preferences.autoSkipOutro) {
        if (v.currentTime >= skipData.outroStart && v.currentTime < skipData.outroEnd - 5) {
          setShowSkipOutro(true)
        } else {
          setShowSkipOutro(false)
        }
      }

      // A-B repeat
      if (isABLooping && abRepeat.b !== null && v.currentTime >= abRepeat.b) {
        v.currentTime = abRepeat.a || 0
      }

      // Auto next episode
      if (preferences.autoNextEpisode && v.currentTime >= v.duration - 30 && v.duration > 60) {
        // Show next episode countdown overlay
      }

      // Memory pressure check
      if ('storage' in navigator && 'estimate' in navigator.storage) {
        navigator.storage.estimate().then(estimate => {
          if (estimate.usage && estimate.quota && estimate.usage / estimate.quota > 0.8) {
            setMemoryWarning(true)
          }
        })
      }
    }

    const onPlay = () => setIsPlaying(true)
    const onPause = () => setIsPlaying(false)
    const onWaiting = () => setIsBuffering(true)
    const onPlaying = () => setIsBuffering(false)
    const onEnded = () => {
      addWatchHistory({
        titleId: title.id,
        episodeId: episode?.id,
        progress: 1,
        duration: v.duration,
        completed: true,
        timestamp: Date.now(),
        watchCount: 1,
      })
    }
    const onVolumeChange = () => {
      setVolume(v.volume)
      setIsMuted(v.muted)
    }
    const onFullscreenChange = () => setIsFullscreen(!!document.fullscreenElement)

    v.addEventListener('timeupdate', onTimeUpdate)
    v.addEventListener('play', onPlay)
    v.addEventListener('pause', onPause)
    v.addEventListener('waiting', onWaiting)
    v.addEventListener('playing', onPlaying)
    v.addEventListener('ended', onEnded)
    v.addEventListener('volumechange', onVolumeChange)
    document.addEventListener('fullscreenchange', onFullscreenChange)

    return () => {
      v.removeEventListener('timeupdate', onTimeUpdate)
      v.removeEventListener('play', onPlay)
      v.removeEventListener('pause', onPause)
      v.removeEventListener('waiting', onWaiting)
      v.removeEventListener('playing', onPlaying)
      v.removeEventListener('ended', onEnded)
      v.removeEventListener('volumechange', onVolumeChange)
      document.removeEventListener('fullscreenchange', onFullscreenChange)
    }
  }, [skipData, preferences, abRepeat, isABLooping, title.id, episode?.id])

  // ─── Controls Auto-Hide ───
  useEffect(() => {
    const onMouseMove = () => {
      setShowControls(true)
      setLastActivity(Date.now())
      clearTimeout(controlsTimeoutRef.current)
      controlsTimeoutRef.current = setTimeout(() => {
        if (isPlaying) setShowControls(false)
      }, 3000)
    }

    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === ' ') { e.preventDefault(); togglePlay() }
      if (e.key === 'f') toggleFullscreen()
      if (e.key === 'm') toggleMute()
      if (e.key === 'ArrowRight') seek(10)
      if (e.key === 'ArrowLeft') seek(-10)
      if (e.key === 'ArrowUp') adjustVolume(0.1)
      if (e.key === 'ArrowDown') adjustVolume(-0.1)
      if (e.key === 'n' && onNext) onNext()
      if (e.key === 'p' && onPrev) onPrev()
      if (e.key === 's') setShowSettings(s => !s)
      if (e.key === 'c') setShowStats(s => !s)
      if (e.key === 'b') toggleBookmark()
      if (e.key === 'a') setShowABMenu(s => !s)
      if (e.key === 'd') setFrameStepMode(s => !s)

      setShowControls(true)
      setLastActivity(Date.now())
      clearTimeout(controlsTimeoutRef.current)
      controlsTimeoutRef.current = setTimeout(() => {
        if (isPlaying) setShowControls(false)
      }, 3000)
    }

    window.addEventListener('mousemove', onMouseMove)
    window.addEventListener('keydown', onKeyDown)

    return () => {
      window.removeEventListener('mousemove', onMouseMove)
      window.removeEventListener('keydown', onKeyDown)
      clearTimeout(controlsTimeoutRef.current)
    }
  }, [isPlaying, onNext, onPrev])

  // ─── Player Stats ───
  useEffect(() => {
    if (!showStats) return
    const interval = setInterval(() => {
      const v = videoRef.current
      if (!v) return

      const fps = v.getVideoPlaybackQuality?.()?.totalVideoFrames / (v.currentTime || 1) || 0
      const dropped = v.getVideoPlaybackQuality?.()?.droppedVideoFrames || 0

      setPlayerStats({
        fps: Math.round(fps),
        dropped,
        bitrate: 0,
        resolution: `${v.videoWidth}x${v.videoHeight}`,
        codec: '',
      })
    }, 1000)
    return () => clearInterval(interval)
  }, [showStats])

  // ─── Upscaling Canvas ───
  useEffect(() => {
    if (!upscalingEnabled || !canvasRef.current || !videoRef.current) return

    const canvas = canvasRef.current
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    const v = videoRef.current
    canvas.width = v.videoWidth * 1.5
    canvas.height = v.videoHeight * 1.5

    let animId: number
    const render = () => {
      if (v.paused || v.ended) { animId = requestAnimationFrame(render); return }
      ctx.drawImage(v, 0, 0, canvas.width, canvas.height)
      // Apply sharpening filter
      if (preset.sharpness > 0) {
        ctx.filter = `contrast(${preset.contrast}%) saturate(${preset.saturation}%) brightness(${preset.brightness}%)`
      }
      animId = requestAnimationFrame(render)
    }
    render()

    return () => cancelAnimationFrame(animId)
  }, [upscalingEnabled, preset])

  // ─── Actions ───
  const togglePlay = useCallback(() => {
    const v = videoRef.current
    if (!v) return
    v.paused ? v.play() : v.pause()
  }, [])

  const seek = useCallback((delta: number) => {
    const v = videoRef.current
    if (!v) return
    v.currentTime = Math.max(0, Math.min(v.duration, v.currentTime + delta))
  }, [])

  const seekTo = useCallback((time: number) => {
    const v = videoRef.current
    if (!v) return
    v.currentTime = time
  }, [])

  const adjustVolume = useCallback((delta: number) => {
    const v = videoRef.current
    if (!v) return
    v.volume = Math.max(0, Math.min(1, v.volume + delta))
  }, [])

  const toggleMute = useCallback(() => {
    const v = videoRef.current
    if (!v) return
    v.muted = !v.muted
  }, [])

  const toggleFullscreen = useCallback(() => {
    if (screenfull.isEnabled && containerRef.current) {
      screenfull.toggle(containerRef.current)
    }
  }, [])

  const changeSpeed = useCallback((rate: number) => {
    const v = videoRef.current
    if (!v) return
    v.playbackRate = rate
    setPlaybackRate(rate)
  }, [])

  const changeQuality = useCallback((q: string) => {
    if (!hlsRef.current) return
    if (q === 'auto') {
      hlsRef.current.currentLevel = -1
    } else {
      const level = hlsRef.current.levels.findIndex(l => `${l.height}p` === q)
      if (level >= 0) hlsRef.current.currentLevel = level
    }
    setQuality(q)
  }, [])

  const loadSubtitle = useCallback(async (track: SubtitleTrack) => {
    try {
      const res = await fetch(track.url)
      const text = await res.text()
      let cues
      if (track.format === 'ass') cues = parseASS(text)
      else if (track.format === 'vtt') cues = parseVTT(text)
      else cues = parseSRT(text)
      setSubtitleCues(cues)
      setActiveSubtitle(track)
    } catch {
      setSubtitleCues([])
    }
  }, [])

  const toggleBookmark = useCallback(() => {
    const v = videoRef.current
    if (!v) return
    const time = v.currentTime
    const existing = bookmarks.find(b => Math.abs(b.time - time) < 5)
    if (existing) {
      removeBookmark(existing.id)
      setBookmarks(getBookmarks(title.id, episode?.id))
    } else {
      addBookmark({
        id: Math.random().toString(36).substring(2),
        titleId: title.id,
        episodeId: episode?.id,
        time,
        label: `Bookmark at ${formatDuration(time)}`,
        createdAt: Date.now(),
      })
      setBookmarks(getBookmarks(title.id, episode?.id))
    }
  }, [bookmarks, title.id, episode?.id])

  const setABPoint = useCallback((point: 'a' | 'b') => {
    const v = videoRef.current
    if (!v) return
    setAbRepeat(prev => ({ ...prev, [point]: v.currentTime }))
  }, [])

  const toggleABLoop = useCallback(() => {
    if (abRepeat.a !== null && abRepeat.b !== null) {
      setIsABLooping(!isABLooping)
      if (!isABLooping && videoRef.current) {
        videoRef.current.currentTime = abRepeat.a
      }
    }
  }, [abRepeat, isABLooping])

  const stepFrame = useCallback((direction: 1 | -1) => {
    const v = videoRef.current
    if (!v) return
    v.pause()
    const frameTime = 1 / 24
    v.currentTime = Math.max(0, v.currentTime + direction * frameTime)
  }, [])

  const skipIntro = useCallback(() => {
    if (skipData && videoRef.current) {
      videoRef.current.currentTime = skipData.introEnd
      setShowSkipIntro(false)
    }
  }, [skipData])

  const skipOutro = useCallback(() => {
    if (skipData && videoRef.current) {
      videoRef.current.currentTime = skipData.outroEnd
      setShowSkipOutro(false)
      if (onNext) onNext()
    }
  }, [skipData, onNext])

  const togglePip = useCallback(async () => {
    const v = videoRef.current
    if (!v) return
    if (document.pictureInPictureElement) {
      await document.exitPictureInPicture()
      setPipMode(false)
    } else {
      await v.requestPictureInPicture()
      setPipMode(true)
    }
  }, [])

  // ─── Current Subtitle Text ───
  const currentSubtitleText = useMemo(() => {
    const v = videoRef.current
    if (!v || subtitleCues.length === 0) return ''
    const cue = subtitleCues.find(c => v.currentTime >= c.start + subtitleOffset && v.currentTime <= c.end + subtitleOffset)
    return cue?.text || ''
  }, [currentTime, subtitleCues, subtitleOffset])

  const progress = duration > 0 ? (currentTime / duration) * 100 : 0
  const bufferProgress = duration > 0 ? (buffered / duration) * 100 : 0

  return (
    <div 
      ref={containerRef}
      className="relative w-full h-full bg-black overflow-hidden select-none"
      onClick={() => {
        setShowControls(true)
        setLastActivity(Date.now())
        clearTimeout(controlsTimeoutRef.current)
        controlsTimeoutRef.current = setTimeout(() => {
          if (isPlaying) setShowControls(false)
        }, 3000)
      }}
    >
      {/* Video Element */}
      <video
        ref={videoRef}
        className={cn("w-full h-full object-contain", upscalingEnabled && "hidden")}
        playsInline
        preload={deviceProfile === 'LOW' ? 'metadata' : 'auto'}
        crossOrigin="anonymous"
        style={{ filter: `brightness(${preset.brightness}%) contrast(${preset.contrast}%) saturate(${preset.saturation}%)` }}
      />

      {/* Upscaling Canvas */}
      {upscalingEnabled && (
        <canvas ref={canvasRef} className="w-full h-full object-contain" />
      )}

      {/* Buffering Spinner */}
      {isBuffering && (
        <div className="absolute inset-0 flex items-center justify-center bg-black/50 z-20">
          <div className="w-12 h-12 border-2 border-primary/30 border-t-primary rounded-full animate-spin" />
        </div>
      )}

      {/* Memory Warning */}
      {memoryWarning && (
        <div className="absolute top-4 right-4 z-50 flex items-center gap-2 px-3 py-1.5 rounded-full bg-warning/20 border border-warning/30 text-warning text-xs">
          <Thermometer className="w-3.5 h-3.5" />
          Memory pressure detected
        </div>
      )}

      {/* Subtitle Display */}
      {currentSubtitleText && (
        <div className="absolute bottom-20 left-1/2 -translate-x-1/2 z-30 max-w-[80%] text-center">
          <div className="px-4 py-2 rounded-lg bg-black/70 backdrop-blur-sm text-white text-lg font-medium leading-relaxed shadow-lg"
            style={{ textShadow: '0 1px 4px rgba(0,0,0,0.8)' }}>
            {currentSubtitleText}
          </div>
        </div>
      )}

      {/* Skip Intro Button */}
      {showSkipIntro && (
        <button
          onClick={skipIntro}
          className="absolute bottom-24 right-4 z-40 flex items-center gap-2 px-4 py-2 rounded-xl bg-primary/90 text-white text-sm font-semibold shadow-lg hover:bg-primary transition-all animate-in slide-in-from-right"
        >
          <SkipForward className="w-4 h-4" />
          Skip Intro
        </button>
      )}

      {/* Skip Outro Button */}
      {showSkipOutro && (
        <button
          onClick={skipOutro}
          className="absolute bottom-24 right-4 z-40 flex items-center gap-2 px-4 py-2 rounded-xl bg-accent/90 text-white text-sm font-semibold shadow-lg hover:bg-accent transition-all animate-in slide-in-from-right"
        >
          <SkipForward className="w-4 h-4" />
          Skip Outro
        </button>
      )}

      {/* Player Stats Overlay */}
      {showStats && (
        <div className="absolute top-4 left-4 z-40 space-y-1 px-3 py-2 rounded-xl fuse-glass text-xs font-mono text-text-muted">
          <div className="flex items-center gap-2"><Gauge className="w-3 h-3 text-primary" /> FPS: {playerStats.fps}</div>
          <div className="flex items-center gap-2"><Cpu className="w-3 h-3 text-primary" /> Dropped: {playerStats.dropped}</div>
          <div className="flex items-center gap-2"><Monitor className="w-3 h-3 text-primary" /> {playerStats.resolution}</div>
          <div className="flex items-center gap-2"><Aperture className="w-3 h-3 text-primary" /> {quality}</div>
          {source.type === 'torrent' && (
            <>
              <div>Peers: {torrentPeers}</div>
              <div>Progress: {torrentProgress.toFixed(1)}%</div>
              <div>Speed: {(torrentSpeed / 1024 / 1024).toFixed(1)} MB/s</div>
            </>
          )}
        </div>
      )}

      {/* Controls Overlay */}
      <div className={cn(
        "absolute inset-0 z-30 flex flex-col justify-between transition-opacity duration-300 bg-gradient-to-t from-black/80 via-transparent to-black/40",
        showControls ? "opacity-100" : "opacity-0 pointer-events-none"
      )}>
        {/* Top Bar */}
        <div className="flex items-center justify-between p-4">
          <div className="flex items-center gap-3">
            <button onClick={onClose} className="p-2 rounded-xl hover:bg-white/10 transition-colors">
              <ChevronRight className="w-5 h-5 rotate-180" />
            </button>
            <div>
              <h2 className="text-sm font-semibold text-white">{title.title}</h2>
              {episode && <p className="text-xs text-text-muted">S{episode.season || 1} E{episode.number} - {episode.title}</p>}
            </div>
          </div>
          <div className="flex items-center gap-2">
            <button onClick={() => setShowStats(s => !s)} className={cn("p-2 rounded-xl transition-colors", showStats && "bg-primary/20 text-primary")}>
              <Gauge className="w-4 h-4" />
            </button>
            <button onClick={togglePip} className={cn("p-2 rounded-xl transition-colors", pipMode && "bg-primary/20 text-primary")}>
              <PictureInPicture2 className="w-4 h-4" />
            </button>
            <button onClick={toggleFullscreen} className="p-2 rounded-xl hover:bg-white/10 transition-colors">
              {isFullscreen ? <Minimize className="w-4 h-4" /> : <Maximize className="w-4 h-4" />}
            </button>
          </div>
        </div>

        {/* Center Controls */}
        <div className="absolute inset-0 flex items-center justify-center gap-6 pointer-events-none">
          <button onClick={() => seek(-10)} className="p-3 rounded-full bg-black/40 backdrop-blur-sm hover:bg-black/60 transition-all pointer-events-auto">
            <Rewind className="w-6 h-6" />
          </button>
          <button onClick={togglePlay} className="p-4 rounded-full bg-primary/90 hover:bg-primary transition-all pointer-events-auto shadow-lg shadow-primary/30">
            {isPlaying ? <Pause className="w-8 h-8" /> : <Play className="w-8 h-8 ml-0.5" />}
          </button>
          <button onClick={() => seek(10)} className="p-3 rounded-full bg-black/40 backdrop-blur-sm hover:bg-black/60 transition-all pointer-events-auto">
            <FastForward className="w-6 h-6" />
          </button>
        </div>

        {/* Bottom Controls */}
        <div className="p-4 space-y-3">
          {/* Progress Bar */}
          <div className="relative group">
            <div className="h-1 bg-white/20 rounded-full overflow-hidden">
              <div className="h-full bg-white/30 rounded-full" style={{ width: `${bufferProgress}%` }} />
              <div className="absolute top-0 h-full bg-primary rounded-full transition-all" style={{ width: `${progress}%` }} />
            </div>
            <input
              type="range"
              min={0}
              max={duration || 100}
              value={currentTime}
              onChange={(e) => seekTo(parseFloat(e.target.value))}
              className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
            />
            {/* Chapter Markers */}
            {chapters.map((ch, i) => (
              <div
                key={i}
                className="absolute top-0 w-0.5 h-2 bg-white/40 -mt-0.5 rounded-full"
                style={{ left: `${(ch.time / (duration || 1)) * 100}%` }}
                title={ch.label}
              />
            ))}
            {/* Bookmark Markers */}
            {bookmarks.map((b) => (
              <div
                key={b.id}
                className="absolute top-0 w-1 h-2 bg-gold -mt-0.5 rounded-full"
                style={{ left: `${(b.time / (duration || 1)) * 100}%` }}
              />
            ))}
            {/* A-B Markers */}
            {abRepeat.a !== null && (
              <div className="absolute top-0 w-1 h-2 bg-accent -mt-0.5 rounded-full" style={{ left: `${(abRepeat.a / (duration || 1)) * 100}%` }} />
            )}
            {abRepeat.b !== null && (
              <div className="absolute top-0 w-1 h-2 bg-accent -mt-0.5 rounded-full" style={{ left: `${(abRepeat.b / (duration || 1)) * 100}%` }} />
            )}
          </div>

          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <span className="text-xs font-mono text-text-muted">{formatDuration(currentTime)}</span>
              <span className="text-xs text-text-dim">/</span>
              <span className="text-xs font-mono text-text-muted">{formatDuration(duration)}</span>

              {/* Volume */}
              <div className="flex items-center gap-2 group">
                <button onClick={toggleMute} className="p-1.5 rounded-lg hover:bg-white/10 transition-colors">
                  {isMuted || volume === 0 ? <VolumeX className="w-4 h-4" /> : <Volume2 className="w-4 h-4" />}
                </button>
                <input
                  type="range"
                  min={0}
                  max={1}
                  step={0.05}
                  value={isMuted ? 0 : volume}
                  onChange={(e) => {
                    const v = videoRef.current
                    if (v) { v.volume = parseFloat(e.target.value); v.muted = false }
                  }}
                  className="w-20 h-1 accent-primary"
                />
              </div>
            </div>

            <div className="flex items-center gap-1">
              {onPrev && (
                <button onClick={onPrev} className="p-2 rounded-xl hover:bg-white/10 transition-colors" title="Previous">
                  <SkipBack className="w-4 h-4" />
                </button>
              )}
              {onNext && (
                <button onClick={onNext} className="p-2 rounded-xl hover:bg-white/10 transition-colors" title="Next">
                  <SkipForward className="w-4 h-4" />
                </button>
              )}

              {/* A-B Repeat */}
              <button onClick={() => setShowABMenu(s => !s)} className={cn("p-2 rounded-xl transition-colors", isABLooping && "bg-accent/20 text-accent")} title="A-B Repeat">
                <RotateCcw className="w-4 h-4" />
              </button>

              {/* Frame Step */}
              <button onClick={() => stepFrame(-1)} className="p-2 rounded-xl hover:bg-white/10 transition-colors" title="Frame Back">
                <Rewind className="w-4 h-4" />
              </button>
              <button onClick={() => stepFrame(1)} className="p-2 rounded-xl hover:bg-white/10 transition-colors" title="Frame Forward">
                <FastForward className="w-4 h-4" />
              </button>

              {/* Bookmarks */}
              <button onClick={toggleBookmark} className="p-2 rounded-xl hover:bg-white/10 transition-colors" title="Bookmark">
                <Bookmark className={cn("w-4 h-4", bookmarks.some(b => Math.abs(b.time - currentTime) < 5) && "fill-gold text-gold")} />
              </button>

              {/* Speed */}
              <button onClick={() => setShowSpeedMenu(s => !s)} className="p-2 rounded-xl hover:bg-white/10 transition-colors text-xs font-mono" title="Speed">
                {playbackRate}x
              </button>

              {/* Subtitles */}
              <button onClick={() => setShowSubMenu(s => !s)} className={cn("p-2 rounded-xl transition-colors", activeSubtitle && "bg-primary/20 text-primary")} title="Subtitles">
                <Subtitles className="w-4 h-4" />
              </button>

              {/* Quality */}
              <button onClick={() => setShowQualityMenu(s => !s)} className="p-2 rounded-xl hover:bg-white/10 transition-colors text-xs font-mono" title="Quality">
                {quality}
              </button>

              {/* Settings */}
              <button onClick={() => setShowSettings(s => !s)} className="p-2 rounded-xl hover:bg-white/10 transition-colors" title="Settings">
                <Settings className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Settings Menu */}
      {showSettings && (
        <div className="absolute right-4 bottom-20 z-50 w-64 rounded-2xl fuse-glass-strong p-4 space-y-3 max-h-[60vh] overflow-y-auto">
          <h3 className="text-sm font-semibold">Player Settings</h3>

          <div className="space-y-2">
            <label className="text-xs text-text-muted">Playback Preset</label>
            <div className="grid grid-cols-2 gap-1">
              {presets.map(p => (
                <button
                  key={p.id}
                  onClick={() => { setPreset(p); setUpscalingEnabled(p.upscaling !== 'none') }}
                  className={cn("px-2 py-1.5 rounded-lg text-xs transition-colors", preset.id === p.id ? "bg-primary/20 text-primary" : "hover:bg-white/5")}
                >
                  {p.name}
                </button>
              ))}
            </div>
          </div>

          <div className="space-y-1">
            <label className="text-xs text-text-muted">Brightness: {preset.brightness}%</label>
            <input type="range" min={50} max={150} value={preset.brightness} onChange={e => setPreset(p => ({ ...p, brightness: parseInt(e.target.value) }))} className="w-full h-1 accent-primary" />
          </div>

          <div className="space-y-1">
            <label className="text-xs text-text-muted">Contrast: {preset.contrast}%</label>
            <input type="range" min={50} max={150} value={preset.contrast} onChange={e => setPreset(p => ({ ...p, contrast: parseInt(e.target.value) }))} className="w-full h-1 accent-primary" />
          </div>

          <div className="space-y-1">
            <label className="text-xs text-text-muted">Saturation: {preset.saturation}%</label>
            <input type="range" min={0} max={200} value={preset.saturation} onChange={e => setPreset(p => ({ ...p, saturation: parseInt(e.target.value) }))} className="w-full h-1 accent-primary" />
          </div>

          <div className="space-y-1">
            <label className="text-xs text-text-muted">Sharpness: {preset.sharpness}</label>
            <input type="range" min={0} max={100} value={preset.sharpness} onChange={e => setPreset(p => ({ ...p, sharpness: parseInt(e.target.value) }))} className="w-full h-1 accent-primary" />
          </div>

          <div className="flex items-center justify-between">
            <span className="text-xs text-text-muted">Denoise</span>
            <button onClick={() => setPreset(p => ({ ...p, denoise: !p.denoise }))} className={cn("w-8 h-4 rounded-full transition-colors", preset.denoise ? "bg-primary" : "bg-white/20")}>
              <div className={cn("w-3 h-3 bg-white rounded-full transition-transform", preset.denoise ? "translate-x-4" : "translate-x-0.5")} />
            </button>
          </div>

          <div className="flex items-center justify-between">
            <span className="text-xs text-text-muted">Upscaling</span>
            <button onClick={() => setUpscalingEnabled(!upscalingEnabled)} className={cn("w-8 h-4 rounded-full transition-colors", upscalingEnabled ? "bg-primary" : "bg-white/20")}>
              <div className={cn("w-3 h-3 bg-white rounded-full transition-transform", upscalingEnabled ? "translate-x-4" : "translate-x-0.5")} />
            </button>
          </div>

          <div className="flex items-center justify-between">
            <span className="text-xs text-text-muted">Auto Skip Intro</span>
            <button onClick={() => updatePreferences({ autoSkipIntro: !preferences.autoSkipIntro })} className={cn("w-8 h-4 rounded-full transition-colors", preferences.autoSkipIntro ? "bg-primary" : "bg-white/20")}>
              <div className={cn("w-3 h-3 bg-white rounded-full transition-transform", preferences.autoSkipIntro ? "translate-x-4" : "translate-x-0.5")} />
            </button>
          </div>

          <div className="flex items-center justify-between">
            <span className="text-xs text-text-muted">Auto Skip Outro</span>
            <button onClick={() => updatePreferences({ autoSkipOutro: !preferences.autoSkipOutro })} className={cn("w-8 h-4 rounded-full transition-colors", preferences.autoSkipOutro ? "bg-primary" : "bg-white/20")}>
              <div className={cn("w-3 h-3 bg-white rounded-full transition-transform", preferences.autoSkipOutro ? "translate-x-4" : "translate-x-0.5")} />
            </button>
          </div>

          <div className="flex items-center justify-between">
            <span className="text-xs text-text-muted">Auto Next Episode</span>
            <button onClick={() => updatePreferences({ autoNextEpisode: !preferences.autoNextEpisode })} className={cn("w-8 h-4 rounded-full transition-colors", preferences.autoNextEpisode ? "bg-primary" : "bg-white/20")}>
              <div className={cn("w-3 h-3 bg-white rounded-full transition-transform", preferences.autoNextEpisode ? "translate-x-4" : "translate-x-0.5")} />
            </button>
          </div>
        </div>
      )}

      {/* Subtitle Menu */}
      {showSubMenu && (
        <div className="absolute right-16 bottom-20 z-50 w-56 rounded-2xl fuse-glass-strong p-4 space-y-3 max-h-[60vh] overflow-y-auto">
          <h3 className="text-sm font-semibold">Subtitles</h3>

          <button onClick={() => { setActiveSubtitle(null); setSubtitleCues([]) }} className={cn("w-full text-left px-3 py-2 rounded-lg text-sm transition-colors", !activeSubtitle && "bg-primary/20 text-primary")}>
            Off
          </button>

          {subtitles.map(sub => (
            <button
              key={sub.id}
              onClick={() => { loadSubtitle(sub); setShowSubMenu(false) }}
              className={cn("w-full text-left px-3 py-2 rounded-lg text-sm transition-colors", activeSubtitle?.id === sub.id && "bg-primary/20 text-primary")}
            >
              <div className="flex items-center justify-between">
                <span>{sub.label}</span>
                <span className="text-xs text-text-dim uppercase">{sub.language}</span>
              </div>
              {sub.isSDH && <span className="text-[10px] text-text-dim">SDH</span>}
            </button>
          ))}

          <div className="pt-2 border-t border-border">
            <label className="text-xs text-text-muted">Sync Offset: {subtitleOffset}s</label>
            <input type="range" min={-10} max={10} step={0.1} value={subtitleOffset} onChange={e => setSubtitleOffset(parseFloat(e.target.value))} className="w-full h-1 accent-primary" />
          </div>
        </div>
      )}

      {/* Speed Menu */}
      {showSpeedMenu && (
        <div className="absolute right-28 bottom-20 z-50 w-32 rounded-2xl fuse-glass-strong p-2 space-y-1">
          {[0.5, 0.75, 1, 1.25, 1.5, 1.75, 2].map(rate => (
            <button
              key={rate}
              onClick={() => { changeSpeed(rate); setShowSpeedMenu(false) }}
              className={cn("w-full text-left px-3 py-2 rounded-lg text-sm transition-colors", playbackRate === rate && "bg-primary/20 text-primary")}
            >
              {rate}x
            </button>
          ))}
        </div>
      )}

      {/* Quality Menu */}
      {showQualityMenu && (
        <div className="absolute right-10 bottom-20 z-50 w-32 rounded-2xl fuse-glass-strong p-2 space-y-1">
          {availableQualities.map(q => (
            <button
              key={q}
              onClick={() => { changeQuality(q); setShowQualityMenu(false) }}
              className={cn("w-full text-left px-3 py-2 rounded-lg text-sm transition-colors", quality === q && "bg-primary/20 text-primary")}
            >
              {q}
            </button>
          ))}
        </div>
      )}

      {/* A-B Menu */}
      {showABMenu && (
        <div className="absolute right-40 bottom-20 z-50 w-48 rounded-2xl fuse-glass-strong p-4 space-y-2">
          <h3 className="text-sm font-semibold">A-B Repeat</h3>
          <div className="flex gap-2">
            <button onClick={() => setABPoint('a')} className={cn("flex-1 px-3 py-2 rounded-lg text-sm transition-colors", abRepeat.a !== null ? "bg-primary/20 text-primary" : "bg-white/5")}>
              Set A {abRepeat.a !== null && `(${formatDuration(abRepeat.a)})`}
            </button>
            <button onClick={() => setABPoint('b')} className={cn("flex-1 px-3 py-2 rounded-lg text-sm transition-colors", abRepeat.b !== null ? "bg-accent/20 text-accent" : "bg-white/5")}>
              Set B {abRepeat.b !== null && `(${formatDuration(abRepeat.b)})`}
            </button>
          </div>
          <button onClick={toggleABLoop} disabled={abRepeat.a === null || abRepeat.b === null} className={cn("w-full px-3 py-2 rounded-lg text-sm transition-colors", isABLooping ? "bg-accent/20 text-accent" : "bg-white/5")}>
            {isABLooping ? 'Stop Loop' : 'Start Loop'}
          </button>
          <button onClick={() => { setAbRepeat({ a: null, b: null }); setIsABLooping(false) }} className="w-full px-3 py-2 rounded-lg text-sm bg-white/5 hover:bg-white/10 transition-colors">
            Clear
          </button>
        </div>
      )}
    </div>
  )
}
