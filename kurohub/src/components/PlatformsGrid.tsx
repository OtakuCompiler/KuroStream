import { Tv, Smartphone, Monitor, CheckCircle2, Clock } from "lucide-react";
import { m } from "framer-motion";

type Status = "available" | "soon";

type Platform = {
  name: string;
  status: Status;
  note: string;
};

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

export function PlatformsGrid() {
  return (
    <section className="relative py-24 md:py-32 content-visibility-auto">
      <div className="mx-auto max-w-6xl px-5">
        <header className="max-w-2xl mb-14">
          <m.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="af-chip mb-4"
          >
            <span className="w-1.5 h-1.5 rounded-full bg-secondary" /> Platforms
          </m.div>
          <m.h2
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: 0.1 }}
            className="font-display text-4xl md:text-5xl font-bold tracking-tight"
          >
            Every screen you own. <span className="text-gradient">One library.</span>
          </m.h2>
          <m.p
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: 0.2 }}
            className="mt-3 text-muted-foreground"
          >
            KuroStream leads on TV and follows everywhere else. Your watchlist, skins, and
            extensions travel with you.
          </m.p>
        </header>

        <div className="grid md:grid-cols-3 gap-6">
          {groups.map((g, gi) => (
            <m.div
              key={g.key}
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: gi * 0.15, duration: 0.5 }}
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
                    whileInView={{ opacity: 1, x: 0 }}
                    viewport={{ once: true }}
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
      </div>
    </section>
  );
}
