import { Link } from "@tanstack/react-router";
import { ArrowRight, Sparkles, Star } from "lucide-react";
import { useListings } from "@/lib/marketplace";
import { m } from "framer-motion";
import { useRef } from "react";

function TiltCard({ children, className }: { children: React.ReactNode; className?: string }) {
  const ref = useRef<HTMLDivElement>(null);

  const handleMouseMove = (e: React.MouseEvent) => {
    if (!ref.current) return;
    const rect = ref.current.getBoundingClientRect();
    const x = (e.clientX - rect.left) / rect.width;
    const y = (e.clientY - rect.top) / rect.height;
    ref.current.style.setProperty("--mouse-x", `${x * 100}%`);
    ref.current.style.setProperty("--mouse-y", `${y * 100}%`);
    const rotateX = (y - 0.5) * -10;
    const rotateY = (x - 0.5) * 10;
    ref.current.style.transform = `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) scale3d(1.02, 1.02, 1.02)`;
  };

  const handleMouseLeave = () => {
    if (!ref.current) return;
    ref.current.style.transform = "perspective(1000px) rotateX(0) rotateY(0) scale3d(1, 1, 1)";
  };

  return (
    <div
      ref={ref}
      onMouseMove={handleMouseMove}
      onMouseLeave={handleMouseLeave}
      className={`spotlight-card transition-transform duration-200 ease-out ${className}`}
      style={{ transformStyle: "preserve-3d" }}
    >
      {children}
    </div>
  );
}

export function FeaturedSkins() {
  const { data: all = [], isLoading } = useListings();
  const featured = all.filter((e) => e.category === "skin").slice(0, 8);

  return (
    <section className="relative py-24 md:py-32 content-visibility-auto">
      <div className="mx-auto max-w-6xl px-5">
        <header className="flex flex-wrap items-end justify-between gap-4 mb-12">
          <div className="max-w-xl">
            <m.div
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              className="af-chip mb-4"
            >
              <Sparkles className="w-3 h-3 text-secondary" /> Marketplace
            </m.div>
            <m.h2
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: 0.1 }}
              className="font-display text-4xl md:text-5xl font-bold tracking-tight"
            >
              Premium <span className="text-gradient">skins</span>, curated.
            </m.h2>
            <m.p
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: 0.2 }}
              className="mt-3 text-muted-foreground"
            >
              Anime-inspired themes crafted for the ten-foot experience. Buy once, apply on every
              device.
            </m.p>
          </div>
          <m.div
            initial={{ opacity: 0, x: 20 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
            transition={{ delay: 0.3 }}
          >
            <Link
              to="/marketplace"
              className="focusable inline-flex items-center gap-2 text-sm text-primary hover:text-primary/80 transition-colors group"
            >
              Browse all{" "}
              <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
            </Link>
          </m.div>
        </header>

        {isLoading ? (
          <div className="grid grid-flow-col auto-cols-[minmax(260px,1fr)] gap-4 overflow-x-auto no-scrollbar pb-3">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="h-56 rounded-xl animate-shimmer" />
            ))}
          </div>
        ) : featured.length === 0 ? (
          <p className="text-sm text-muted-foreground">No skins yet — check back soon.</p>
        ) : (
          <div className="grid grid-flow-col auto-cols-[minmax(280px,1fr)] gap-5 overflow-x-auto no-scrollbar pb-4 snap-x snap-mandatory">
            {featured.map((ext, i) => {
              const p = ext.palette ?? {
                primary: "#BB86FC",
                secondary: "#03DAC6",
                bg: "#121212",
                accent: "#CF6679",
              };
              const bg = `radial-gradient(120% 90% at 20% 10%, ${p.primary}55, transparent 60%), radial-gradient(120% 90% at 90% 90%, ${p.secondary}44, transparent 55%), linear-gradient(180deg, ${p.bg}, #050505)`;
              return (
                <m.div
                  key={ext.id}
                  initial={{ opacity: 0, y: 30 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: i * 0.08, duration: 0.5 }}
                >
                  <TiltCard className="h-full">
                    <Link
                      to="/marketplace/$id"
                      params={{ id: ext.id }}
                      className="block h-64 rounded-2xl overflow-hidden relative group snap-start"
                      style={{ background: bg }}
                    >
                      <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent" />
                      <div className="absolute bottom-0 left-0 right-0 p-5">
                        <div className="flex items-center gap-2 mb-2">
                          <span className="text-lg">{ext.emoji}</span>
                          <h3 className="font-display font-semibold text-sm">{ext.name}</h3>
                        </div>
                        <p className="text-xs text-white/70 line-clamp-2 mb-3">{ext.description}</p>
                        <div className="flex items-center justify-between">
                          <span className="text-xs font-medium">
                            {ext.price === 0 ? "Free" : `$${ext.price.toFixed(2)}`}
                          </span>
                          <span className="flex items-center gap-1 text-xs text-white/60">
                            <Star className="w-3 h-3 fill-current" /> {ext.rating.toFixed(1)}
                          </span>
                        </div>
                      </div>
                      <div className="absolute top-3 right-3 opacity-0 group-hover:opacity-100 transition-opacity">
                        <div className="w-8 h-8 rounded-full bg-white/10 backdrop-blur-sm flex items-center justify-center">
                          <ArrowRight className="w-4 h-4 text-white" />
                        </div>
                      </div>
                    </Link>
                  </TiltCard>
                </m.div>
              );
            })}
          </div>
        )}
      </div>
    </section>
  );
}
