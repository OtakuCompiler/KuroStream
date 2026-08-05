import { Code2, GitBranch, Terminal, ArrowRight, ExternalLink } from "lucide-react";
import { m } from "framer-motion";

const resources = [
  {
    icon: Code2,
    title: "Extension SDK",
    desc: "Build content adapters in Kotlin or JavaScript. Full type-safe API.",
    href: "https://github.com/OtakuCompiler/KuroStream/tree/main/extensions",
  },
  {
    icon: GitBranch,
    title: "Contribute",
    desc: "PRs welcome. Pick up good-first-issue tags and earn contributor badges.",
    href: "https://github.com/OtakuCompiler/KuroStream/contribute",
  },
  {
    icon: Terminal,
    title: "API Reference",
    desc: "REST endpoints for sync, purchases, and skin management.",
    href: "/docs",
  },
];

export function DeveloperSection() {
  return (
    <section className="relative py-24 md:py-32 content-visibility-auto">
      <div className="mx-auto max-w-6xl px-5">
        <m.header
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="max-w-2xl mb-14"
        >
          <div className="af-chip mb-4">
            <span className="w-1.5 h-1.5 rounded-full bg-secondary" /> Developers
          </div>
          <h2 className="font-display text-4xl md:text-5xl font-bold tracking-tight">
            Built by the community. <span className="text-gradient">For the community.</span>
          </h2>
          <p className="mt-3 text-muted-foreground">
            KuroStream is open source. Every line of code, every skin, every extension is
            community-driven.
          </p>
        </m.header>

        <div className="grid md:grid-cols-3 gap-5">
          {resources.map((r, i) => (
            <m.a
              key={r.title}
              href={r.href}
              target={r.href.startsWith("http") ? "_blank" : undefined}
              rel={r.href.startsWith("http") ? "noreferrer" : undefined}
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: i * 0.1, duration: 0.5 }}
              whileHover={{ y: -4, scale: 1.02 }}
              className="glow-border glass-card p-6 group cursor-pointer block"
            >
              <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center mb-4 group-hover:bg-primary/20 transition-colors">
                <r.icon className="w-5 h-5 text-primary" />
              </div>
              <h3 className="font-display font-semibold text-base mb-2 flex items-center gap-2">
                {r.title}
                <ExternalLink className="w-3.5 h-3.5 text-muted-foreground opacity-0 group-hover:opacity-100 transition-opacity" />
              </h3>
              <p className="text-sm text-muted-foreground leading-relaxed mb-4">{r.desc}</p>
              <span className="text-xs text-primary inline-flex items-center gap-1 group-hover:gap-2 transition-all">
                Learn more <ArrowRight className="w-3 h-3" />
              </span>
            </m.a>
          ))}
        </div>
      </div>
    </section>
  );
}
