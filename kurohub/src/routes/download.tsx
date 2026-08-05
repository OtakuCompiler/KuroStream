import { createFileRoute } from "@tanstack/react-router";
import { m } from "framer-motion";
import {
  Download,
  Github,
  AlertTriangle,
  CheckCircle2,
  Monitor,
  Smartphone,
  Tv,
} from "lucide-react";
import { Button } from "@/components/ui/button";

const platforms = [
  { icon: Tv, name: "Android TV", status: "available", url: "#" },
  { icon: Monitor, name: "Google TV", status: "available", url: "#" },
  { icon: Smartphone, name: "Android Phone", status: "soon", url: null },
];

export const Route = createFileRoute("/download")({
  head: () => ({
    meta: [
      { title: "Download — Kuro Stream" },
      { name: "description", content: "Download KuroStream for Android TV, Google TV, and more." },
    ],
  }),
  component: DownloadPage,
});

function DownloadPage() {
  return (
    <div className="mx-auto max-w-3xl px-5 pt-28 pb-20">
      <m.header
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="text-center mb-12"
      >
        <h1 className="font-display text-4xl md:text-5xl font-bold text-gradient">Download.</h1>
        <p className="mt-3 text-muted-foreground">Get KuroStream on your device.</p>
      </m.header>

      <m.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
        className="glass-card p-6 mb-6 border-amber-400/20 bg-amber-400/5"
      >
        <div className="flex items-start gap-3">
          <AlertTriangle className="w-5 h-5 text-amber-400 flex-shrink-0 mt-0.5" />
          <div>
            <h3 className="font-display font-semibold text-sm text-amber-400">Work in progress</h3>
            <p className="text-sm text-muted-foreground mt-1">
              KuroStream is currently in active development. APK builds are not yet available for
              public download. Follow our GitHub for release notifications.
            </p>
          </div>
        </div>
      </m.div>

      <div className="space-y-3">
        {platforms.map((p, i) => (
          <m.div
            key={p.name}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 + i * 0.08 }}
            className="glass-card p-5 flex items-center justify-between"
          >
            <div className="flex items-center gap-4">
              <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center">
                <p.icon className="w-5 h-5 text-primary" />
              </div>
              <div>
                <h3 className="font-display font-semibold text-sm">{p.name}</h3>
                <p className="text-xs text-muted-foreground">
                  {p.status === "available" ? "Available now" : "Coming soon"}
                </p>
              </div>
            </div>
            {p.url ? (
              <Button size="sm" disabled>
                <Download className="w-4 h-4 mr-1" /> Download
              </Button>
            ) : (
              <span className="text-xs text-muted-foreground px-3 py-1.5 bg-white/5 rounded-lg">
                Soon
              </span>
            )}
          </m.div>
        ))}
      </div>

      <m.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.5 }}
        className="mt-10 text-center"
      >
        <a
          href="https://github.com/OtakuCompiler/KuroStream"
          target="_blank"
          rel="noreferrer"
          className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground transition-colors"
        >
          <Github className="w-4 h-4" /> Follow releases on GitHub
        </a>
      </m.div>
    </div>
  );
}
