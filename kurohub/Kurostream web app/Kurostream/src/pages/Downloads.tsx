import { useStore } from '@/lib/store'
import { cn, formatBytes } from '@/lib/utils'
import { Download, Pause, Play, Trash2, FileVideo } from 'lucide-react'

export default function Downloads() {
  const { downloads, updateDownload, removeDownload } = useStore()

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-8 pt-6 pb-12 space-y-6">
      <h1 className="text-2xl font-bold">Downloads</h1>
      {downloads.length === 0 ? (
        <div className="text-center py-20 text-text-dim">
          <FileVideo className="w-12 h-12 mx-auto mb-3 opacity-30" />
          <p>No active downloads</p>
          <p className="text-sm mt-1">Torrent and HTTP downloads appear here</p>
        </div>
      ) : (
        <div className="space-y-3">
          {downloads.map(item => (
            <div key={item.id} className="p-4 rounded-2xl bg-surface border border-border">
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-primary/10 flex items-center justify-center">
                    <Download className="w-4 h-4 text-primary" />
                  </div>
                  <div>
                    <p className="text-sm font-medium truncate max-w-[200px] sm:max-w-md">{item.title}</p>
                    <p className="text-xs text-text-dim">{item.type.toUpperCase()}</p>
                  </div>
                </div>
                <div className="flex items-center gap-1">
                  {item.status === 'downloading' && <button onClick={() => updateDownload(item.id, { status: 'paused' })} className="p-1.5 rounded-lg hover:bg-white/5 transition-colors"><Pause className="w-4 h-4" /></button>}
                  {item.status === 'paused' && <button onClick={() => updateDownload(item.id, { status: 'downloading' })} className="p-1.5 rounded-lg hover:bg-white/5 transition-colors"><Play className="w-4 h-4" /></button>}
                  <button onClick={() => removeDownload(item.id)} className="p-1.5 rounded-lg hover:bg-danger/10 text-danger transition-colors"><Trash2 className="w-4 h-4" /></button>
                </div>
              </div>
              <div className="space-y-1">
                <div className="flex items-center justify-between text-xs text-text-dim">
                  <span>{item.status}</span>
                  <span>{formatBytes(item.downloaded)} / {formatBytes(item.size)}</span>
                </div>
                <div className="h-1.5 bg-white/10 rounded-full overflow-hidden">
                  <div className={cn("h-full rounded-full transition-all", item.status === 'completed' ? "bg-success" : "bg-primary")} style={{ width: `${item.progress}%` }} />
                </div>
                {item.status === 'downloading' && <p className="text-xs text-text-dim">{formatBytes(item.speed)}/s · {item.peers || 0} peers</p>}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
