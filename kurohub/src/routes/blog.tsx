import { createFileRoute } from "@tanstack/react-router";
import { m } from "framer-motion";
import { BookOpen, Calendar, ArrowRight } from "lucide-react";

const posts = [
  {
    title: "Introducing KuroStream 2.0",
    date: "2026-07-15",
    excerpt:
      "A complete rewrite with Jetpack Compose, dual-player engine, and the new extension system.",
    tag: "Release",
  },
  {
    title: "Building skins for the ten-foot experience",
    date: "2026-06-28",
    excerpt: "Design principles for TV interfaces: contrast, focus states, and D-pad navigation.",
    tag: "Design",
  },
  {
    title: "Why we chose GPLv3",
    date: "2026-06-10",
    excerpt: "Our commitment to open source and why copyleft matters for media players.",
    tag: "Community",
  },
];

export const Route = createFileRoute("/blog")({
  head: () => ({
    meta: [
      { title: "Blog — Kuro Stream" },
      {
        name: "description",
        content: "Updates, tutorials, and behind-the-scenes from the KuroStream team.",
      },
    ],
  }),
  component: BlogPage,
});

function BlogPage() {
  return (
    <div className="mx-auto max-w-3xl px-5 pt-28 pb-20">
      <m.header initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="mb-12">
        <div className="af-chip mb-4">
          <BookOpen className="w-3 h-3 text-primary" /> Blog
        </div>
        <h1 className="font-display text-4xl font-bold text-gradient">Updates & insights.</h1>
        <p className="mt-2 text-muted-foreground">From the KuroStream team and community.</p>
      </m.header>

      <div className="space-y-6">
        {posts.map((post, i) => (
          <m.article
            key={post.title}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.1 }}
            whileHover={{ x: 4 }}
            className="glass-card p-6 group cursor-pointer"
          >
            <div className="flex items-center gap-3 mb-3">
              <span className="af-chip text-[10px]">{post.tag}</span>
              <span className="flex items-center gap-1 text-xs text-muted-foreground">
                <Calendar className="w-3 h-3" /> {post.date}
              </span>
            </div>
            <h2 className="font-display font-semibold text-lg mb-2 group-hover:text-primary transition-colors">
              {post.title}
            </h2>
            <p className="text-sm text-muted-foreground leading-relaxed mb-3">{post.excerpt}</p>
            <span className="text-xs text-primary inline-flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
              Read more <ArrowRight className="w-3 h-3" />
            </span>
          </m.article>
        ))}
      </div>
    </div>
  );
}
