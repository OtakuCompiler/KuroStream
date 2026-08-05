import { Download, Plug, Palette, Clapperboard } from "lucide-react";
import { m } from "framer-motion";

const steps = [
  {
    icon: Download,
    title: "Install",
    body: "Grab the APK from GitHub or sideload via ADB. One file, no Play Store dependency.",
  },
  {
    icon: Plug,
    title: "Add extensions",
    body: "Install metadata, subtitle, sync, and library extensions from the marketplace. They auto-update.",
  },
  {
    icon: Palette,
    title: "Pick a skin",
    body: "Browse the marketplace, claim free themes, or unlock the Skins Pass for everything.",
  },
  {
    icon: Clapperboard,
    title: "Press play",
    body: "Lean back. The player picks the best source, skips intros, and queues the next episode.",
  },
];

export function HowItWorks() {
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
            <span className="w-1.5 h-1.5 rounded-full bg-primary" /> How it works
          </m.div>
          <m.h2
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: 0.1 }}
            className="font-display text-4xl md:text-5xl font-bold tracking-tight"
          >
            From zero to <span className="text-gradient">binge</span> in four steps.
          </m.h2>
        </header>

        <div className="relative grid md:grid-cols-4 gap-8 md:gap-6">
          {/* Timeline line - desktop only */}
          <div className="hidden md:block absolute top-10 left-[12.5%] right-[12.5%] h-0.5 bg-gradient-to-r from-primary/30 via-secondary/30 to-primary/30" />

          {steps.map((s, i) => (
            <m.div
              key={s.title}
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: i * 0.15, duration: 0.5 }}
              className="relative text-center"
            >
              <m.div
                whileHover={{ scale: 1.1, rotate: 5 }}
                className="w-16 h-16 mx-auto rounded-2xl bg-primary/10 border border-primary/20 flex items-center justify-center mb-5 relative z-10"
              >
                <s.icon className="w-7 h-7 text-primary" />
                <div className="absolute -top-1 -right-1 w-6 h-6 rounded-full bg-background border border-primary/30 flex items-center justify-center text-[10px] font-bold text-primary">
                  {i + 1}
                </div>
              </m.div>
              <h3 className="font-display font-semibold text-base mb-2">{s.title}</h3>
              <p className="text-sm text-muted-foreground leading-relaxed">{s.body}</p>
            </m.div>
          ))}
        </div>
      </div>
    </section>
  );
}
