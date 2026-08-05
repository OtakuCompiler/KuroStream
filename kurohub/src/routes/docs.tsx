import { createFileRoute } from "@tanstack/react-router";
import { m } from "framer-motion";
import { BookOpen, Terminal, FileCode, Zap, ExternalLink } from "lucide-react";

const sections = [
  {
    title: "Getting Started",
    items: [
      { label: "Installation", desc: "Download and install KuroStream on your Android TV device." },
      {
        label: "First Launch",
        desc: "Initial setup, permissions, and connecting your AniList account.",
      },
      {
        label: "Adding Extensions",
        desc: "How to install marketplace extensions for metadata, subtitles, and sync.",
      },
    ],
  },
  {
    title: "Development",
    items: [
      { label: "Extension SDK", desc: "Build content adapters in Kotlin or JavaScript." },
      { label: "Skin System", desc: "Create custom themes with our declarative skin format." },
      { label: "API Reference", desc: "REST endpoints for sync, purchases, and entitlements." },
    ],
  },
  {
    title: "Advanced",
    items: [
      { label: "Self-hosting", desc: "Deploy your own KuroStream sync server on Cloudflare." },
      { label: "Troubleshooting", desc: "Common issues and how to resolve them." },
      { label: "Contributing", desc: "Code style, PR guidelines, and commit conventions." },
    ],
  },
];

export const Route = createFileRoute("/docs")({
  head: () => ({
    meta: [
      { title: "Documentation — Kuro Stream" },
      {
        name: "description",
        content: "KuroStream documentation, API reference, and developer guides.",
      },
    ],
  }),
  component: DocsPage,
});

function DocsPage() {
  return (
    <div className="mx-auto max-w-4xl px-5 pt-28 pb-20">
      <m.header initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="mb-12">
        <div className="af-chip mb-4">
          <BookOpen className="w-3 h-3 text-primary" /> Documentation
        </div>
        <h1 className="font-display text-4xl font-bold text-gradient">Docs & API.</h1>
        <p className="mt-2 text-muted-foreground">
          Everything you need to use and extend KuroStream.
        </p>
      </m.header>

      <div className="space-y-10">
        {sections.map((section, si) => (
          <m.div
            key={section.title}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: si * 0.1 }}
          >
            <h2 className="font-display text-lg font-semibold mb-4">{section.title}</h2>
            <div className="space-y-3">
              {section.items.map((item, ii) => (
                <m.div
                  key={item.label}
                  initial={{ opacity: 0, x: -10 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: si * 0.1 + ii * 0.05 }}
                  whileHover={{ x: 4 }}
                  className="glass-card p-4 flex items-center justify-between group cursor-pointer"
                >
                  <div>
                    <h3 className="font-medium text-sm group-hover:text-primary transition-colors">
                      {item.label}
                    </h3>
                    <p className="text-xs text-muted-foreground">{item.desc}</p>
                  </div>
                  <ExternalLink className="w-4 h-4 text-muted-foreground/30 group-hover:text-muted-foreground transition-colors" />
                </m.div>
              ))}
            </div>
          </m.div>
        ))}
      </div>
    </div>
  );
}
