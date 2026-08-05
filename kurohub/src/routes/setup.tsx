import { createFileRoute } from "@tanstack/react-router";
import { m } from "framer-motion";
import { Download, Settings, Plug, Palette, Play, CheckCircle2 } from "lucide-react";
import { StepCard } from "@/components/StepCard";

const steps = [
  {
    icon: Download,
    title: "Install",
    body: "Download the APK from GitHub releases or sideload via ADB. One file, no Play Store dependency.",
    step: 1,
  },
  {
    icon: Settings,
    title: "Configure",
    body: "Set up your preferences, choose default player, and connect AniList for watchlist sync.",
    step: 2,
  },
  {
    icon: Plug,
    title: "Add extensions",
    body: "Install metadata, subtitle, sync, and library extensions from the marketplace. They auto-update.",
    step: 3,
  },
  {
    icon: Palette,
    title: "Pick a skin",
    body: "Browse the marketplace, claim free themes, or unlock the Skins Pass for everything.",
    step: 4,
  },
  {
    icon: Play,
    title: "Enjoy",
    body: "Lean back. The player picks the best source, skips intros, and queues the next episode.",
    step: 5,
  },
];

export const Route = createFileRoute("/setup")({
  head: () => ({
    meta: [
      { title: "Setup Guide — Kuro Stream" },
      {
        name: "description",
        content: "Step-by-step guide to install and configure KuroStream on your Android TV.",
      },
    ],
  }),
  component: SetupPage,
});

function SetupPage() {
  return (
    <div className="mx-auto max-w-4xl px-5 pt-28 pb-20">
      <m.header
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="text-center mb-14"
      >
        <h1 className="font-display text-4xl md:text-5xl font-bold text-gradient">Setup Guide.</h1>
        <p className="mt-3 text-muted-foreground max-w-lg mx-auto">
          Get KuroStream running on your Android TV in under 5 minutes.
        </p>
      </m.header>

      <div className="grid md:grid-cols-2 gap-5 mb-12">
        {steps.map((s, i) => (
          <m.div
            key={s.title}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.1 }}
          >
            <StepCard {...s} />
          </m.div>
        ))}
      </div>

      <m.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.5 }}
        className="glass-card p-8 text-center"
      >
        <CheckCircle2 className="w-10 h-10 text-emerald-400 mx-auto mb-4" />
        <h2 className="font-display text-xl font-bold mb-2">Need help?</h2>
        <p className="text-sm text-muted-foreground mb-4">
          Join our community for support, troubleshooting, and tips.
        </p>
        <div className="flex justify-center gap-3">
          <a
            href="https://github.com/OtakuCompiler/KuroStream"
            target="_blank"
            rel="noreferrer"
            className="px-4 py-2 rounded-lg bg-white/5 text-sm hover:bg-white/10 transition-colors"
          >
            GitHub Issues
          </a>
        </div>
      </m.div>
    </div>
  );
}
