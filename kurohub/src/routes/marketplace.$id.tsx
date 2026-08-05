import { useState } from "react";
import { createFileRoute, Link, useParams } from "@tanstack/react-router";
import { m } from "framer-motion";
import {
  ArrowLeft,
  Star,
  Download,
  Check,
  ShoppingCart,
  Sparkles,
  Shield,
  Zap,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  useListing,
  useLibrary,
  useUser,
  SKINS_PASS_ID,
  SKINS_PASS_PRICE,
} from "@/lib/marketplace";
import { toast } from "sonner";

export const Route = createFileRoute("/marketplace/$id")({
  head: ({ params }: { params: { id: string } }) => ({
    meta: [
      { title: `Item — Kuro Stream Marketplace` },
      {
        name: "description",
        content: "View details and purchase this KuroStream skin or extension.",
      },
    ],
  }),
  component: ItemPage,
});

function ItemPage() {
  const { id } = useParams({ from: "/marketplace/$id" });
  const { data: ext, isLoading } = useListing(id);
  const { user } = useUser();
  const { purchases, claimFree, purchase } = useLibrary();
  const owned = new Set(purchases.map((p) => p.item_id));
  const isOwned = owned.has(id);
  const isPass = id === SKINS_PASS_ID;
  const [purchasing, setPurchasing] = useState(false);

  if (isLoading) {
    return (
      <div className="mx-auto max-w-4xl px-5 pt-28 pb-20">
        <div className="h-8 w-32 rounded animate-shimmer mb-4" />
        <div className="h-96 rounded-2xl animate-shimmer" />
      </div>
    );
  }

  if (!ext) {
    return (
      <div className="mx-auto max-w-4xl px-5 pt-28 pb-20 text-center">
        <m.h1 initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="font-display text-2xl">
          Not found
        </m.h1>
        <m.p
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.1 }}
          className="text-muted-foreground mt-2"
        >
          This item doesn't exist or has been removed.
        </m.p>
        <m.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.2 }}>
          <Link to="/marketplace" className="inline-flex mt-6 text-primary hover:underline">
            ← Back to marketplace
          </Link>
        </m.div>
      </div>
    );
  }

  const p = ext.palette ?? {
    primary: "#BB86FC",
    secondary: "#03DAC6",
    bg: "#121212",
    accent: "#CF6679",
  };
  const bg = `radial-gradient(120% 90% at 20% 10%, ${p.primary}33, transparent 60%), radial-gradient(120% 90% at 90% 90%, ${p.secondary}22, transparent 55%), linear-gradient(180deg, ${p.bg}, #050505)`;

  return (
    <div className="mx-auto max-w-4xl px-5 pt-28 pb-20">
      <m.div initial={{ opacity: 0, x: -10 }} animate={{ opacity: 1, x: 0 }}>
        <Link
          to="/marketplace"
          className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors mb-6 group"
        >
          <ArrowLeft className="w-4 h-4 group-hover:-translate-x-1 transition-transform" /> Back
        </Link>
      </m.div>

      <m.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="grid md:grid-cols-2 gap-8"
      >
        {/* Preview */}
        <div
          className="rounded-2xl overflow-hidden aspect-square md:aspect-auto md:h-full min-h-[300px] relative"
          style={{ background: bg }}
        >
          <div className="absolute inset-0 flex items-center justify-center">
            <m.span
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              transition={{ delay: 0.3, type: "spring" }}
              className="text-8xl"
            >
              {ext.emoji}
            </m.span>
          </div>
          <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
          <div className="absolute bottom-4 left-4 right-4 flex items-center justify-between">
            <div className="flex items-center gap-1 text-xs text-white/70">
              <Star className="w-3 h-3 fill-amber-400 text-amber-400" /> {ext.rating.toFixed(1)}
            </div>
            <div className="flex items-center gap-1 text-xs text-white/70">
              <Download className="w-3 h-3" /> {ext.installs.toLocaleString()}
            </div>
          </div>
        </div>

        {/* Details */}
        <div>
          <m.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
          >
            <div className="flex items-center gap-2 mb-3">
              <span className="af-chip text-[10px]">{ext.category}</span>
              {ext.isPremium && (
                <span className="af-chip text-[10px] text-amber-400 border-amber-400/30">
                  <Sparkles className="w-3 h-3" /> Premium
                </span>
              )}
            </div>
            <h1 className="font-display text-3xl font-bold">{ext.name}</h1>
            <p className="text-sm text-muted-foreground mt-1">by {ext.author}</p>
          </m.div>

          <m.p
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.2 }}
            className="text-sm text-muted-foreground leading-relaxed mt-4"
          >
            {ext.longDescription || ext.description}
          </m.p>

          {/* Features */}
          <m.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.3 }}
            className="mt-6 space-y-2"
          >
            {[
              { icon: Zap, text: "Instant download" },
              { icon: Shield, text: "Lifetime updates" },
              { icon: Check, text: "Works on all devices" },
            ].map(({ icon: Icon, text }) => (
              <div key={text} className="flex items-center gap-2 text-sm text-muted-foreground">
                <Icon className="w-4 h-4 text-primary/60" /> {text}
              </div>
            ))}
          </m.div>

          {/* Price & Action */}
          <m.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.4 }}
            className="mt-8 pt-6 border-t border-border/20"
          >
            <div className="flex items-end justify-between mb-4">
              <div>
                <div className="font-display text-3xl font-bold text-gradient">
                  {isPass
                    ? `$${SKINS_PASS_PRICE.toFixed(2)}`
                    : ext.price === 0
                      ? "Free"
                      : `$${ext.price.toFixed(2)}`}
                </div>
                {isPass && (
                  <p className="text-xs text-muted-foreground">One-time payment, lifetime access</p>
                )}
              </div>
            </div>

            {isOwned ? (
              <div className="flex items-center gap-2 text-sm text-emerald-400 bg-emerald-400/10 border border-emerald-400/20 rounded-lg px-4 py-3">
                <Check className="w-4 h-4" /> You own this
              </div>
            ) : ext.price === 0 ? (
              <m.div whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.98 }}>
                <Button
                  onClick={() => claimFree(ext.id)}
                  className="w-full h-12 text-base bg-primary hover:bg-primary/90"
                >
                  <Download className="w-4 h-4 mr-2" /> Claim Free
                </Button>
              </m.div>
            ) : (
              <m.div whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.98 }}>
                <Button
                  onClick={() => {
                    if (!user) return;
                    setPurchasing(true);
                    purchase(ext.id)
                      .catch((e) => {
                        toast.error(e.message || "Purchase failed");
                      })
                      .finally(() => setPurchasing(false));
                  }}
                  disabled={!user || purchasing}
                  className="w-full h-12 text-base bg-primary hover:bg-primary/90 disabled:opacity-50"
                >
                  <ShoppingCart className="w-4 h-4 mr-2" />
                  {user
                    ? purchasing
                      ? "Redirecting…"
                      : `Buy for $${(isPass ? SKINS_PASS_PRICE : ext.price).toFixed(2)}`
                    : "Sign in to purchase"}
                </Button>
              </m.div>
            )}

            {!user && (
              <p className="text-xs text-muted-foreground mt-3 text-center">
                <Link to="/auth" className="text-primary hover:underline">
                  Sign in
                </Link>{" "}
                to purchase and sync across devices.
              </p>
            )}
          </m.div>
        </div>
      </m.div>
    </div>
  );
}
