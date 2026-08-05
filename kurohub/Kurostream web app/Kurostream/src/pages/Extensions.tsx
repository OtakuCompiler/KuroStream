import { useState } from 'react'
import { cn } from '@/lib/utils'
import { useStore } from '@/lib/store'
import { BUILT_IN_EXTENSIONS } from '@/lib/extensions/engine'
import { Puzzle, Download, Check, Star, Shield, Globe } from 'lucide-react'

export default function Extensions() {
  const { extensions, installExtension, uninstallExtension, toggleExtension } = useStore()
  const [activeFilter, setActiveFilter] = useState<string>('all')

  const allExtensions = [...BUILT_IN_EXTENSIONS, ...extensions]
  const filtered = activeFilter === 'all' ? allExtensions : allExtensions.filter(e => e.type === activeFilter)

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-8 pt-6 pb-12 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Extension Marketplace</h1>
        <div className="flex items-center gap-2">
          {['all', 'provider', 'subtitle', 'metadata', 'theme', 'widget'].map(f => (
            <button key={f} onClick={() => setActiveFilter(f)} className={cn("px-3 py-1.5 rounded-lg text-xs font-medium capitalize transition-colors", activeFilter === f ? "bg-primary/15 text-primary" : "bg-white/5 text-text-muted hover:bg-white/10")}>{f}</button>
          ))}
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {filtered.map(ext => {
          const installed = extensions.find(e => e.id === ext.id)
          const isEnabled = installed?.enabled
          return (
            <div key={ext.id} className="p-4 rounded-2xl bg-surface border border-border hover:border-border-strong transition-all group">
              <div className="flex items-start justify-between mb-3">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center">
                    <Puzzle className="w-5 h-5 text-primary" />
                  </div>
                  <div>
                    <h3 className="font-medium text-sm">{ext.name}</h3>
                    <p className="text-xs text-text-dim">v{ext.version} · {ext.author}</p>
                  </div>
                </div>
                {ext.rating && <div className="flex items-center gap-1 text-xs text-gold"><Star className="w-3 h-3 fill-gold" />{ext.rating}</div>}
              </div>
              <p className="text-xs text-text-muted mb-3 line-clamp-2">{ext.description}</p>
              <div className="flex flex-wrap gap-1.5 mb-3">
                <span className="px-2 py-0.5 rounded-md bg-white/5 text-[10px] uppercase tracking-wider text-text-dim">{ext.type}</span>
                {ext.permissions.map(p => (
                  <span key={p} className="px-2 py-0.5 rounded-md bg-white/5 text-[10px] text-text-dim flex items-center gap-1">
                    {p === 'network' ? <Globe className="w-2.5 h-2.5" /> : <Shield className="w-2.5 h-2.5" />}
                    {p}
                  </span>
                ))}
              </div>
              <div className="flex gap-2">
                {installed ? (
                  <>
                    <button onClick={() => toggleExtension(ext.id)} className={cn("flex-1 flex items-center justify-center gap-1.5 px-3 py-2 rounded-xl text-xs font-medium transition-colors", isEnabled ? "bg-success/15 text-success" : "bg-white/5 text-text-muted")}>
                      {isEnabled ? <Check className="w-3.5 h-3.5" /> : <Shield className="w-3.5 h-3.5" />}
                      {isEnabled ? 'Enabled' : 'Disabled'}
                    </button>
                    <button onClick={() => uninstallExtension(ext.id)} className="px-3 py-2 rounded-xl bg-danger/10 text-danger text-xs font-medium hover:bg-danger/20 transition-colors">Remove</button>
                  </>
                ) : (
                  <button onClick={() => installExtension(ext)} className="w-full flex items-center justify-center gap-1.5 px-3 py-2 rounded-xl bg-primary/15 text-primary text-xs font-medium hover:bg-primary/25 transition-colors">
                    <Download className="w-3.5 h-3.5" />
                    Install
                  </button>
                )}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
