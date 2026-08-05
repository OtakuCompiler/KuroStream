import { createFileRoute, Outlet, useMatches } from "@tanstack/react-router";
import { m } from "framer-motion";
import { useListings } from "@/lib/marketplace";
import { ExtCard } from "@/components/ExtCard";
import { SkinsPassBanner } from "@/components/SkinsPassBanner";
import { Search, Filter, Grid3X3, List } from "lucide-react";
import { useState } from "react";

export const Route = createFileRoute("/marketplace")({
  head: () => ({
    meta: [
      { title: "Marketplace — Kuro Stream" },
      {
        name: "description",
        content: "Browse premium original skins and extensions for KuroStream.",
      },
    ],
  }),
  component: MarketplaceLayout,
});

function MarketplaceLayout() {
  const matches = useMatches();
  const isDetail = matches.some((m) => m.routeId?.includes("$id"));
  if (isDetail) return <Outlet />;
  return <MarketplacePage />;
}

function MarketplacePage() {
  const { data: all = [], isLoading } = useListings();
  const [search, setSearch] = useState("");
  const [category, setCategory] = useState<"all" | "skin" | "pack" | "addon">("all");
  const [view, setView] = useState<"grid" | "list">("grid");

  const filtered = all.filter((e) => {
    const matchSearch =
      e.name.toLowerCase().includes(search.toLowerCase()) ||
      e.description.toLowerCase().includes(search.toLowerCase());
    const matchCat = category === "all" || e.category === category;
    return matchSearch && matchCat;
  });

  const categories: { key: "all" | "skin" | "pack" | "addon"; label: string }[] = [
    { key: "all", label: "All" },
    { key: "skin", label: "Skins" },
    { key: "pack", label: "Packs" },
    { key: "addon", label: "Addons" },
  ];

  return (
    <div className="mx-auto max-w-6xl px-5 pt-28 pb-20">
      <m.header
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="mb-10"
      >
        <h1 className="font-display text-4xl md:text-5xl font-bold text-gradient">Marketplace</h1>
        <p className="mt-2 text-muted-foreground">
          Premium skins and extensions for your KuroStream setup.
        </p>
      </m.header>

      <SkinsPassBanner />

      {/* Filters */}
      <m.div
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.2 }}
        className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8"
      >
        <div className="relative flex-1 max-w-md w-full">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground/50" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search skins, packs..."
            className="w-full pl-10 pr-4 py-2.5 bg-input/60 border border-border/50 rounded-lg text-sm focus:ring-2 focus:ring-primary/40 transition-all outline-none"
          />
        </div>
        <div className="flex items-center gap-2">
          {categories.map((c) => (
            <button
              key={c.key}
              onClick={() => setCategory(c.key)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
                category === c.key
                  ? "bg-primary/20 text-primary border border-primary/30"
                  : "text-muted-foreground hover:text-foreground hover:bg-white/5 border border-transparent"
              }`}
            >
              {c.label}
            </button>
          ))}
          <div className="w-px h-6 bg-border/30 mx-1" />
          <button
            onClick={() => setView("grid")}
            className={`p-2 rounded-lg transition-all ${view === "grid" ? "bg-white/5 text-foreground" : "text-muted-foreground hover:text-foreground"}`}
          >
            <Grid3X3 className="w-4 h-4" />
          </button>
          <button
            onClick={() => setView("list")}
            className={`p-2 rounded-lg transition-all ${view === "list" ? "bg-white/5 text-foreground" : "text-muted-foreground hover:text-foreground"}`}
          >
            <List className="w-4 h-4" />
          </button>
        </div>
      </m.div>

      {isLoading ? (
        <div className={view === "grid" ? "grid sm:grid-cols-2 lg:grid-cols-3 gap-5" : "space-y-3"}>
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="h-56 rounded-2xl animate-shimmer" />
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <m.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="text-center py-20">
          <p className="text-muted-foreground">No items found.</p>
        </m.div>
      ) : (
        <div className={view === "grid" ? "grid sm:grid-cols-2 lg:grid-cols-3 gap-5" : "space-y-3"}>
          {filtered.map((ext, i) => (
            <m.div
              key={ext.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.05 }}
            >
              <ExtCard ext={ext} />
            </m.div>
          ))}
        </div>
      )}
    </div>
  );
}
