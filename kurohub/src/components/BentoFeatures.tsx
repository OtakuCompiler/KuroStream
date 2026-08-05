import {
  Tv2,
  Layers,
  Cpu,
  Puzzle,
  Network,
  SkipForward,
  RefreshCcw,
  Subtitles,
  Lock,
  Film,
  Repeat,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { m } from "framer-motion";

interface Card {
  icon: LucideIcon;
  title: string;
  body: string;
  className: string;
}

const CARDS: Card[] = [
  {
    icon: Tv2,
    title: "TV-first UI",
    body: "Jetpack Compose Leanback. Full D-pad. High-contrast focus states. Zero jank.",
    className: "md:col-span-2 md:row-span-2",
  },
  {
    icon: Network,
    title: "Cloud-first playback",
    body: "Stream straight from Google Drive, OneDrive, and Dropbox with smart local caching.",
    className: "md:col-span-2",
  },
  {
    icon: Cpu,
    title: "1 GB RAM ready",
    body: "8s buffer caps, HW decoder priority, memory pooling.",
    className: "",
  },
  {
    icon: Layers,
    title: "Dual-player engine",
    body: "ExoPlayer + VLC fallback for exotic codecs.",
    className: "",
  },
  {
    icon: Puzzle,
    title: "Extension ecosystem",
    body: "Open plugin SDK for metadata, subtitles, sync, and library tools.",
    className: "md:col-span-2",
  },
  {
    icon: RefreshCcw,
    title: "Trakt & AniList sync",
    body: "OAuth2 watchlist auto-scrobble at 80%.",
    className: "",
  },
  {
    icon: SkipForward,
    title: "Skip timestamps",
    body: "AniSkip + IntroDB overlay, one D-pad press.",
    className: "",
  },
  {
    icon: Repeat,
    title: "Auto next episode",
    body: "Smart episode-index bump, seamless binge playback.",
    className: "md:col-span-2",
  },
  {
    icon: Subtitles,
    title: "Multi-source subs",
    body: "OpenSubtitles + SubDL aggregation, dedupe, ±5s sync slider.",
    className: "",
  },
  {
    icon: Lock,
    title: "Profile isolation",
    body: "Per-profile extensions, history, PIN lock.",
    className: "",
  },
  {
    icon: Film,
    title: "Local library",
    body: "Folder browsing, metadata matching, and custom trailer art for your own files.",
    className: "md:col-span-2",
  },
];

export function BentoFeatures() {
  return (
    <section className="relative mx-auto max-w-6xl px-5 py-24 md:py-32 content-visibility-auto">
      <header className="max-w-2xl mb-14">
        <m.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="af-chip mb-4"
        >
          <span className="w-1.5 h-1.5 rounded-full bg-secondary" /> Features
        </m.div>
        <m.h2
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ delay: 0.1 }}
          className="font-display text-4xl md:text-5xl font-bold tracking-tight"
        >
          Built for the <span className="text-gradient">living room</span>.
        </m.h2>
        <m.p
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ delay: 0.2 }}
          className="mt-3 text-muted-foreground"
        >
          Every decision optimised for D-pad navigation and constrained hardware.
        </m.p>
      </header>

      <div className="grid auto-rows-[180px] gap-4 md:grid-cols-4">
        {CARDS.map(({ icon: Icon, title, body, className }, i) => (
          <m.div
            key={title}
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, margin: "-50px" }}
            transition={{ delay: i * 0.04, duration: 0.5 }}
            whileHover={{ scale: 1.02, y: -3 }}
            className={`glow-border af-panel p-5 flex flex-col justify-between cursor-default ${className}`}
          >
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-lg bg-primary/10 flex items-center justify-center group-hover:bg-primary/20 transition-colors">
                <Icon className="w-4.5 h-4.5 text-primary" />
              </div>
              <h3 className="font-display font-semibold text-sm">{title}</h3>
            </div>
            <p className="text-xs text-muted-foreground leading-relaxed">{body}</p>
          </m.div>
        ))}
      </div>
    </section>
  );
}
