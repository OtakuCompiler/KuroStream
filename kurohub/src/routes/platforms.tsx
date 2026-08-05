import { createFileRoute, Link } from "@tanstack/react-router";
import { m } from "framer-motion";
import { Tv, Smartphone, Monitor, CheckCircle2, Clock, ArrowRight } from "lucide-react";

type Platform = { name: string; status: "available" | "soon"; note: string };

const groups: { key: string; title: string; icon: typeof Tv; blurb: string; items: Platform[] }[] =
  [
    {
      key: "tv",
      title: "Television",
      icon: Tv,
      blurb: "The primary target — every layout is D-pad first, focus-visible, and 10-ft ready.",
      items: [
        { name: "Android TV", status: "available", note: "1 GB RAM+, HW decode" },
        { name: "Google TV", status: "available", note: "Full Leanback UI" },
        { name: "Fire TV", status: "available", note: "Gen 1 → Gen 4K Max" },
        { name: "NVIDIA Shield", status: "available", note: "4K HDR, Dolby" },
        { name: "Chromecast", status: "soon", note: "with Google TV" },
        { name: "LG webOS", status: "soon", note: "Native client planned" },
        { name: "Samsung Tizen", status: "soon", note: "Native client planned" },
        { name: "Apple TV", status: "soon", note: "tvOS companion" },
      ],
    },
    {
      key: "mobile",
      title: "Mobile",
      icon: Smartphone,
      blurb: "Same engine, hand-held. Same account, same library, same skins.",
      items: [
        { name: "Android Phone", status: "soon", note: "Companion + cast" },
        { name: "Android Tablet", status: "soon", note: "Adaptive layout" },
        { name: "iOS", status: "soon", note: "SwiftUI companion" },
        { name: "iPadOS", status: "soon", note: "Split-view ready" },
      ],
    },
    {
      key: "desktop",
      title: "Desktop",
      icon: Monitor,
      blurb: "Full-fat playback with keyboard shortcuts and multi-monitor support.",
      items: [
        { name: "Windows", status: "soon", note: "11 / 10 · x64 + ARM" },
        { name: "macOS", status: "soon", note: "13+ · Apple Silicon" },
        { name: "Linux", status: "soon", note: "AppImage / Flatpak" },
      ],
    },
  ];

export const Route = createFileRoute("/platforms")({
  head: () => ({
    meta: [
      { title: "Platforms — Kuro Stream" },
      {
        name: "description",
        content: "KuroStream platform support: Android TV, Google TV, Fire TV, and more.",
      },
    ],
  }),
  component: PlatformsPage,
});

function PlatformsPage() {
  return (
    <div className="mx-auto max-w-6xl px-5 pt-28 pb-20">
      <m.header
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="text-center mb-14"
      >
        <h1 className="font-display text-4xl md:text-5xl font-bold text-gradient">Platforms.</h1>
        <p className="mt-3 text-muted-foreground max-w-lg mx-auto">
          KuroStream leads on TV and follows everywhere else.
        </p>
      </m.header>

      <div className="grid md:grid-cols-3 gap-6">
        {groups.map((g, gi) => (
          <m.div
            key={g.key}
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: gi * 0.15 }}
            className="glass-card p-6"
          >
            <div className="flex items-center gap-3 mb-3">
              <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center">
                <g.icon className="w-5 h-5 text-primary" />
              </div>
              <h3 className="font-display font-semibold text-lg">{g.title}</h3>
            </div>
            <p className="text-sm text-muted-foreground mb-5">{g.blurb}</p>
            <ul className="space-y-2.5">
              {g.items.map((item, ii) => (
                <m.li
                  key={item.name}
                  initial={{ opacity: 0, x: -10 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: gi * 0.15 + ii * 0.05 }}
                  className="flex items-center justify-between text-sm"
                >
                  <span className="flex items-center gap-2">
                    {item.status === "available" ? (
                      <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                    ) : (
                      <Clock className="w-3.5 h-3.5 text-amber-400" />
                    )}
                    {item.name}
                  </span>
                  <span className="text-xs text-muted-foreground/60">{item.note}</span>
                </m.li>
              ))}
            </ul>
          </m.div>
        ))}
      </div>

      <m.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.5 }}
        className="mt-12 text-center"
      >
        <Link
          to="/download"
          className="inline-flex items-center gap-2 px-5 py-2.5 rounded-lg bg-primary text-primary-foreground text-sm font-medium hover:bg-primary/90 transition-colors"
        >
          Download <ArrowRight className="w-4 h-4" />
        </Link>
      </m.div>
    </div>
  );
}
