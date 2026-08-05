import { createFileRoute } from "@tanstack/react-router";
import { m } from "framer-motion";
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
  Zap,
} from "lucide-react";
import { FeatureGrid } from "@/components/FeatureGrid";

const featureGroups = [
  {
    title: "Playback",
    items: [
      {
        icon: Tv2,
        title: "TV-first UI",
        body: "Jetpack Compose Leanback with full D-pad support and high-contrast focus states.",
      },
      {
        icon: Cpu,
        title: "Dual-player engine",
        body: "ExoPlayer primary with VLC fallback for exotic codecs and containers.",
      },
      {
        icon: Layers,
        title: "4K HDR",
        body: "Hardware-accelerated decoding with HDR10 and Dolby Vision support.",
      },
      { icon: Zap, title: "Fast seek", body: "Sub-second seeking with pre-buffered keyframes." },
    ],
  },
  {
    title: "Content",
    items: [
      {
        icon: Puzzle,
        title: "Extensions",
        body: "Open plugin SDK for metadata, subtitles, sync, and library tools. Community-driven, auto-updating.",
      },
      {
        icon: Network,
        title: "Cloud-first playback",
        body: "Stream from Google Drive, OneDrive, and Dropbox with smart local caching.",
      },
      {
        icon: Film,
        title: "Local library",
        body: "Folder browsing and metadata matching for your own media collection.",
      },
      {
        icon: Subtitles,
        title: "Multi-source subs",
        body: "Aggregate, deduplicate, and sync subtitles from multiple sources.",
      },
    ],
  },
  {
    title: "Experience",
    items: [
      {
        icon: SkipForward,
        title: "Skip intros",
        body: "AniSkip and IntroDB integration. One D-pad press to skip.",
      },
      {
        icon: Repeat,
        title: "Auto next",
        body: "Seamless episode queueing with smart index bumping.",
      },
      {
        icon: RefreshCcw,
        title: "Trakt & AniList sync",
        body: "OAuth2 watchlist with auto-scrobble at 80% completion.",
      },
      {
        icon: Lock,
        title: "Profile isolation",
        body: "Per-profile addons, history, and PIN lock.",
      },
    ],
  },
];

export const Route = createFileRoute("/features")({
  head: () => ({
    meta: [
      { title: "Features — Kuro Stream" },
      {
        name: "description",
        content:
          "Explore KuroStream features: dual-player engine, extensions, AniList sync, and more.",
      },
    ],
  }),
  component: FeaturesPage,
});

function FeaturesPage() {
  return (
    <div className="mx-auto max-w-5xl px-5 pt-28 pb-20">
      <m.header
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="text-center mb-14"
      >
        <h1 className="font-display text-4xl md:text-5xl font-bold text-gradient">Features.</h1>
        <p className="mt-3 text-muted-foreground max-w-lg mx-auto">
          Everything that makes KuroStream the best anime player for Android TV.
        </p>
      </m.header>

      <div className="space-y-16">
        {featureGroups.map((group, gi) => (
          <m.div
            key={group.title}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: gi * 0.1 }}
          >
            <h2 className="font-display text-xl font-semibold mb-6">{group.title}</h2>
            <FeatureGrid features={group.items} />
          </m.div>
        ))}
      </div>
    </div>
  );
}
