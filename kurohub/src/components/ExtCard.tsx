import { Link } from "@tanstack/react-router";
import { Star, Download, ArrowRight } from "lucide-react";
import { m } from "framer-motion";
import type { Extension } from "@/lib/marketplace";

export function ExtCard({ ext }: { ext: Extension }) {
  const p = ext.palette ?? {
    primary: "#BB86FC",
    secondary: "#03DAC6",
    bg: "#121212",
    accent: "#CF6679",
  };
  const bg = `radial-gradient(120% 90% at 20% 10%, ${p.primary}44, transparent 60%), radial-gradient(120% 90% at 90% 90%, ${p.secondary}33, transparent 55%), linear-gradient(180deg, ${p.bg}, #050505)`;

  return (
    <m.div
      whileHover={{ y: -6, scale: 1.02 }}
      transition={{ type: "spring", stiffness: 300, damping: 20 }}
    >
      <Link
        to="/marketplace/$id"
        params={{ id: ext.id }}
        className="block rounded-2xl overflow-hidden relative group h-full"
        style={{ background: bg }}
      >
        <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent" />
        <div className="relative p-5 h-full flex flex-col justify-end min-h-[220px]">
          <div className="flex items-center gap-2 mb-2">
            <span className="text-lg">{ext.emoji}</span>
            <h3 className="font-display font-semibold text-sm">{ext.name}</h3>
          </div>
          <p className="text-xs text-white/60 line-clamp-2 mb-3">{ext.description}</p>
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium">
              {ext.price === 0 ? "Free" : `$${ext.price.toFixed(2)}`}
            </span>
            <div className="flex items-center gap-2 text-xs text-white/50">
              <span className="flex items-center gap-0.5">
                <Star className="w-3 h-3 fill-amber-400 text-amber-400" /> {ext.rating.toFixed(1)}
              </span>
              <span className="flex items-center gap-0.5">
                <Download className="w-3 h-3" /> {ext.installs.toLocaleString()}
              </span>
            </div>
          </div>
        </div>
        <div className="absolute top-3 right-3 opacity-0 group-hover:opacity-100 transition-all duration-300 transform translate-x-2 group-hover:translate-x-0">
          <div className="w-8 h-8 rounded-full bg-white/10 backdrop-blur-sm flex items-center justify-center border border-white/10">
            <ArrowRight className="w-4 h-4 text-white" />
          </div>
        </div>
      </Link>
    </m.div>
  );
}
