import { useState } from 'react'
import { cn } from '@/lib/utils'
import { useStore } from '@/lib/store'
import { Monitor, Smartphone, Tv, Gauge, Lock, Palette, UserPlus, Trash2, Bell, Cast, Users, Cloud } from 'lucide-react'

const SECTIONS = [
  { id: 'profile', label: 'Profile', icon: UserPlus },
  { id: 'playback', label: 'Playback', icon: Gauge },
  { id: 'appearance', label: 'Appearance', icon: Palette },
  { id: 'notifications', label: 'Notifications', icon: Bell },
  { id: 'cast', label: 'Cast & Devices', icon: Cast },
  { id: 'watchparty', label: 'Watch Party', icon: Users },
  { id: 'sync', label: 'Cloud Sync', icon: Cloud },
  { id: 'privacy', label: 'Privacy', icon: Lock },
]

export default function SettingsPage() {
  const { preferences, updatePreferences, profiles, activeProfileId, addProfile, setActiveProfile } = useStore()
  const [activeSection, setActiveSection] = useState('profile')
  const [newProfileName, setNewProfileName] = useState('')

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-8 pt-6 pb-12">
      <h1 className="text-2xl font-bold mb-6">Settings</h1>
      <div className="flex flex-col lg:flex-row gap-6">
        <div className="lg:w-48 flex lg:flex-col gap-1 overflow-x-auto lg:overflow-visible">
          {SECTIONS.map(section => {
            const Icon = section.icon
            return (
              <button
                key={section.id}
                onClick={() => setActiveSection(section.id)}
                className={cn(
                  "flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-medium transition-all whitespace-nowrap",
                  activeSection === section.id ? "bg-primary/15 text-primary" : "text-text-muted hover:bg-white/5 hover:text-text"
                )}
              >
                <Icon className="w-4 h-4" />
                {section.label}
              </button>
            )
          })}
        </div>

        <div className="flex-1 space-y-6">
          {activeSection === 'profile' && (
            <div className="space-y-6">
              <h2 className="text-lg font-semibold">Profiles</h2>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {profiles.map(profile => (
                  <div
                    key={profile.id}
                    onClick={() => setActiveProfile(profile.id)}
                    className={cn(
                      "p-4 rounded-2xl border transition-all cursor-pointer",
                      activeProfileId === profile.id ? "border-primary/30 bg-primary/5" : "border-border bg-surface hover:border-border-strong"
                    )}
                  >
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-full bg-gradient-to-br from-accent to-primary flex items-center justify-center text-sm font-bold">
                        {profile.name[0]}
                      </div>
                      <div>
                        <p className="font-medium">{profile.name}</p>
                        {profile.isKids && <span className="text-xs text-gold">Kids Mode</span>}
                      </div>
                    </div>
                  </div>
                ))}
                <div className="p-4 rounded-2xl border border-dashed border-border bg-surface/50">
                  <input
                    type="text"
                    value={newProfileName}
                    onChange={e => setNewProfileName(e.target.value)}
                    placeholder="New profile name"
                    className="w-full bg-transparent text-sm outline-none placeholder:text-text-dim mb-2"
                    onKeyDown={e => {
                      if (e.key === 'Enter' && newProfileName.trim()) {
                        addProfile({
                          id: Math.random().toString(36).substring(2),
                          name: newProfileName,
                          isKids: false,
                          preferences: preferences,
                          watchHistory: [],
                          library: [],
                          playlists: [],
                        })
                        setNewProfileName('')
                      }
                    }}
                  />
                </div>
              </div>
            </div>
          )}

          {activeSection === 'playback' && (
            <div className="space-y-6">
              <h2 className="text-lg font-semibold">Playback</h2>
              <div className="space-y-4">
                {[
                  { key: 'autoSkipIntro', label: 'Auto Skip Intro', desc: 'Automatically skip opening sequences' },
                  { key: 'autoSkipOutro', label: 'Auto Skip Outro', desc: 'Skip ending credits' },
                  { key: 'autoNextEpisode', label: 'Auto Next Episode', desc: 'Play next episode automatically' },
                ].map(item => (
                  <div key={item.key} className="flex items-center justify-between p-4 rounded-2xl bg-surface border border-border">
                    <div>
                      <p className="font-medium">{item.label}</p>
                      <p className="text-xs text-text-dim">{item.desc}</p>
                    </div>
                    <button
                      onClick={() => updatePreferences({ [item.key]: !(preferences as any)[item.key] })}
                      className={cn("w-10 h-5 rounded-full transition-colors", (preferences as any)[item.key] ? "bg-primary" : "bg-white/20")}
                    >
                      <div className={cn("w-4 h-4 bg-white rounded-full transition-transform", (preferences as any)[item.key] ? "translate-x-5" : "translate-x-0.5")} />
                    </button>
                  </div>
                ))}
                <div className="p-4 rounded-2xl bg-surface border border-border space-y-3">
                  <p className="font-medium">Default Quality</p>
                  <div className="flex flex-wrap gap-2">
                    {(['auto', '4K', '1080p', '720p', '480p'] as const).map(q => (
                      <button key={q} onClick={() => updatePreferences({ defaultQuality: q })} className={cn("px-3 py-1.5 rounded-lg text-sm transition-colors", preferences.defaultQuality === q ? "bg-primary/15 text-primary" : "bg-white/5 text-text-muted hover:bg-white/10")}>{q}</button>
                    ))}
                  </div>
                </div>
                <div className="p-4 rounded-2xl bg-surface border border-border space-y-3">
                  <p className="font-medium">Device Profile</p>
                  <div className="grid grid-cols-2 gap-2">
                    {[{ id: 'LOW', label: 'Low RAM', icon: Gauge }, { id: 'TV', label: 'TV', icon: Tv }, { id: 'MOBILE', label: 'Mobile', icon: Smartphone }, { id: 'DESKTOP', label: 'Desktop', icon: Monitor }].map(d => {
                      const Icon = d.icon
                      return (
                        <button key={d.id} onClick={() => updatePreferences({ deviceProfile: d.id as any })} className={cn("flex items-center gap-2 px-3 py-2.5 rounded-xl text-sm transition-colors", preferences.deviceProfile === d.id ? "bg-primary/15 text-primary border border-primary/20" : "bg-white/5 text-text-muted hover:bg-white/10 border border-transparent")}>
                          <Icon className="w-4 h-4" />
                          {d.label}
                        </button>
                      )
                    })}
                  </div>
                </div>
              </div>
            </div>
          )}

          {activeSection === 'appearance' && (
            <div className="space-y-6">
              <h2 className="text-lg font-semibold">Appearance</h2>
              <div className="p-4 rounded-2xl bg-surface border border-border space-y-3">
                <p className="font-medium">Theme</p>
                <div className="flex flex-wrap gap-2">
                  {(['dark', 'darker', 'midnight', 'arctic'] as const).map(t => (
                    <button key={t} onClick={() => updatePreferences({ theme: t })} className={cn("px-4 py-2 rounded-xl text-sm capitalize transition-colors", preferences.theme === t ? "bg-primary/15 text-primary" : "bg-white/5 text-text-muted hover:bg-white/10")}>{t}</button>
                  ))}
                </div>
              </div>
              <div className="p-4 rounded-2xl bg-surface border border-border space-y-3">
                <p className="font-medium">Card Shape</p>
                <div className="flex flex-wrap gap-2">
                  {(['poster', 'wide', 'square'] as const).map(s => (
                    <button key={s} onClick={() => updatePreferences({ cardShape: s })} className={cn("px-4 py-2 rounded-xl text-sm capitalize transition-colors", preferences.cardShape === s ? "bg-primary/15 text-primary" : "bg-white/5 text-text-muted hover:bg-white/10")}>{s}</button>
                  ))}
                </div>
              </div>
              {[
                { key: 'showRatings', label: 'Show Ratings', desc: 'Display rating badges on cards' },
                { key: 'preferEnglishTitles', label: 'Prefer English Titles', desc: 'Show English titles when available' },
                { key: 'reduceMotion', label: 'Reduce Motion', desc: 'Minimize animations for accessibility' },
              ].map(item => (
                <div key={item.key} className="flex items-center justify-between p-4 rounded-2xl bg-surface border border-border">
                  <div>
                    <p className="font-medium">{item.label}</p>
                    <p className="text-xs text-text-dim">{item.desc}</p>
                  </div>
                  <button onClick={() => updatePreferences({ [item.key]: !(preferences as any)[item.key] })} className={cn("w-10 h-5 rounded-full transition-colors", (preferences as any)[item.key] ? "bg-primary" : "bg-white/20")}>
                    <div className={cn("w-4 h-4 bg-white rounded-full transition-transform", (preferences as any)[item.key] ? "translate-x-5" : "translate-x-0.5")} />
                  </button>
                </div>
              ))}
            </div>
          )}

          {activeSection === 'notifications' && (
            <div className="space-y-6">
              <h2 className="text-lg font-semibold">Notifications</h2>
              <div className="p-4 rounded-2xl bg-surface border border-border">
                <p className="text-sm text-text-muted">New episode notifications are checked locally. No push server required.</p>
              </div>
            </div>
          )}

          {activeSection === 'cast' && (
            <div className="space-y-6">
              <h2 className="text-lg font-semibold">Cast & Devices</h2>
              <div className="p-4 rounded-2xl bg-surface border border-border">
                <p className="text-sm text-text-muted">Chromecast and AirPlay support. Connect to a device to cast media.</p>
              </div>
            </div>
          )}

          {activeSection === 'watchparty' && (
            <div className="space-y-6">
              <h2 className="text-lg font-semibold">Watch Party</h2>
              <div className="p-4 rounded-2xl bg-surface border border-border">
                <p className="text-sm text-text-muted">Synchronized playback with friends. Create or join a party from the player.</p>
              </div>
            </div>
          )}

          {activeSection === 'sync' && (
            <div className="space-y-6">
              <h2 className="text-lg font-semibold">Cloud Sync</h2>
              <div className="p-4 rounded-2xl bg-surface border border-border">
                <p className="text-sm text-text-muted">Trakt.tv integration for watch history sync. Configure your Trakt API key to enable.</p>
              </div>
            </div>
          )}

          {activeSection === 'privacy' && (
            <div className="space-y-6">
              <h2 className="text-lg font-semibold">Privacy & Security</h2>
              <div className="p-4 rounded-2xl bg-surface border border-border space-y-2">
                <p className="font-medium">Local-Only Mode</p>
                <p className="text-xs text-text-muted">All data stays on your device. No cloud sync.</p>
                <div className="flex items-center gap-2 mt-2">
                  <div className="w-2 h-2 rounded-full bg-success" />
                  <span className="text-xs text-success">Active - All processing is local</span>
                </div>
              </div>
              <div className="p-4 rounded-2xl bg-surface border border-border space-y-2">
                <p className="font-medium">Encrypted Storage</p>
                <p className="text-xs text-text-muted">Watch history and preferences are encrypted at rest.</p>
              </div>
              <div className="p-4 rounded-2xl bg-surface border border-border space-y-2">
                <p className="font-medium">Tracker Protection</p>
                <p className="text-xs text-text-muted">No analytics, no telemetry, no tracking.</p>
              </div>
              <button onClick={() => { if (confirm('Clear all local data?')) { localStorage.clear(); window.location.reload() } }} className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-danger/10 text-danger text-sm font-medium hover:bg-danger/20 transition-colors">
                <Trash2 className="w-4 h-4" />
                Clear All Data
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
