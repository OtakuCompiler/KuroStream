import { createFileRoute, Link } from "@tanstack/react-router";
import { m } from "framer-motion";
import { Package, Download, ExternalLink } from "lucide-react";
import { useLibrary, useListings } from "@/lib/marketplace";
import { Button } from "@/components/ui/button";

export const Route = createFileRoute("/library")({
  head: () => ({
    meta: [
      { title: "Library — Kuro Stream" },
      { name: "description", content: "Your purchased skins and extensions." },
    ],
  }),
  component: LibraryPage,
});

function LibraryPage() {
  const { purchases, isLoading } = useLibrary();
  const { data: all = [] } = useListings();
  const ownedItems = all.filter((e) => purchases.some((p) => p.item_id === e.id));

  return (
    <div className="mx-auto max-w-4xl px-5 pt-28 pb-20">
      <m.header initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="mb-10">
        <h1 className="font-display text-4xl font-bold text-gradient">Library</h1>
        <p className="mt-2 text-muted-foreground">Your purchased skins and extensions.</p>
      </m.header>

      {isLoading ? (
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="h-48 rounded-xl animate-shimmer" />
          ))}
        </div>
      ) : ownedItems.length === 0 ? (
        <m.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="glass-card p-10 text-center"
        >
          <Package className="w-12 h-12 text-muted-foreground/30 mx-auto mb-4" />
          <h2 className="font-display text-lg font-semibold mb-2">Your library is empty</h2>
          <p className="text-sm text-muted-foreground mb-6">
            Browse the marketplace to find skins and extensions.
          </p>
          <Button asChild>
            <Link to="/marketplace">Browse marketplace</Link>
          </Button>
        </m.div>
      ) : (
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {ownedItems.map((item, i) => (
            <m.div
              key={item.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.08 }}
              whileHover={{ y: -4 }}
              className="glass-card p-5"
            >
              <div className="flex items-center gap-3 mb-3">
                <span className="text-2xl">{item.emoji}</span>
                <div>
                  <h3 className="font-display font-semibold text-sm">{item.name}</h3>
                  <p className="text-xs text-muted-foreground">{item.category}</p>
                </div>
              </div>
              <p className="text-xs text-muted-foreground mb-4 line-clamp-2">{item.description}</p>
              <div className="flex gap-2">
                <Button size="sm" variant="secondary" className="text-xs h-8">
                  <Download className="w-3 h-3 mr-1" /> Download
                </Button>
                <Button asChild size="sm" variant="ghost" className="text-xs h-8">
                  <Link to="/marketplace/$id" params={{ id: item.id }}>
                    <ExternalLink className="w-3 h-3 mr-1" /> Details
                  </Link>
                </Button>
              </div>
            </m.div>
          ))}
        </div>
      )}
    </div>
  );
}
