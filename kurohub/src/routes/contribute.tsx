import { createFileRoute } from "@tanstack/react-router";
import { m } from "framer-motion";
import { Github, GitPullRequest, Bug, MessageSquare, Code2, Heart } from "lucide-react";

const ways = [
  {
    icon: Code2,
    title: "Code",
    desc: "Submit PRs for features, bug fixes, or performance improvements.",
  },
  {
    icon: GitPullRequest,
    title: "Extensions",
    desc: "Build content adapters using our Kotlin or JavaScript SDK.",
  },
  {
    icon: Bug,
    title: "Report bugs",
    desc: "Open detailed issues with logs and reproduction steps.",
  },
  {
    icon: MessageSquare,
    title: "Community",
    desc: "Help others in Discord, write docs, or translate.",
  },
];

export const Route = createFileRoute("/contribute")({
  head: () => ({
    meta: [
      { title: "Contribute — Kuro Stream" },
      { name: "description", content: "Join the KuroStream open-source community." },
    ],
  }),
  component: ContributePage,
});

function ContributePage() {
  return (
    <div className="mx-auto max-w-4xl px-5 pt-28 pb-20">
      <m.header
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="text-center mb-14"
      >
        <div className="af-chip mb-4 mx-auto">
          <Heart className="w-3 h-3 text-destructive fill-destructive" /> Open Source
        </div>
        <h1 className="font-display text-4xl md:text-5xl font-bold text-gradient">Contribute.</h1>
        <p className="mt-3 text-muted-foreground max-w-lg mx-auto">
          KuroStream is built by the community. Whether you code, design, write, or test — there's a
          place for you.
        </p>
      </m.header>

      <div className="grid md:grid-cols-2 gap-5 mb-12">
        {ways.map((w, i) => (
          <m.div
            key={w.title}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 + i * 0.08 }}
            whileHover={{ y: -4 }}
            className="glass-card p-6"
          >
            <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center mb-4">
              <w.icon className="w-5 h-5 text-primary" />
            </div>
            <h3 className="font-display font-semibold text-sm mb-1">{w.title}</h3>
            <p className="text-sm text-muted-foreground leading-relaxed">{w.desc}</p>
          </m.div>
        ))}
      </div>

      <m.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.4 }}
        className="glass-card p-8 text-center"
      >
        <Github className="w-10 h-10 text-muted-foreground/30 mx-auto mb-4" />
        <h2 className="font-display text-xl font-bold mb-2">Start on GitHub</h2>
        <p className="text-sm text-muted-foreground mb-6">
          Fork the repo, pick an issue, and open your first PR.
        </p>
        <a
          href="https://github.com/OtakuCompiler/KuroStream"
          target="_blank"
          rel="noreferrer"
          className="inline-flex items-center gap-2 px-5 py-2.5 rounded-lg bg-primary text-primary-foreground text-sm font-medium hover:bg-primary/90 transition-colors"
        >
          <Github className="w-4 h-4" /> View on GitHub
        </a>
      </m.div>
    </div>
  );
}
