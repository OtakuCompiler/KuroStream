import { createFileRoute, Link } from "@tanstack/react-router";
import { m } from "framer-motion";
import { DollarSign, Upload, BarChart3, Shield, ArrowRight, CheckCircle2 } from "lucide-react";
import { Button } from "@/components/ui/button";

export const Route = createFileRoute("/seller")({
  head: () => ({
    meta: [
      { title: "Sell on Kuro Stream — Kuro Stream" },
      {
        name: "description",
        content: "Sell skins and extensions on the KuroStream marketplace. Keep 85% of every sale.",
      },
    ],
  }),
  component: SellerPage,
});

const features = [
  {
    icon: DollarSign,
    title: "85% revenue share",
    desc: "You keep the vast majority. We only take 15% to cover platform costs.",
  },
  {
    icon: Upload,
    title: "Easy uploads",
    desc: "Drag and drop your skin files. We handle packaging, distribution, and updates.",
  },
  {
    icon: BarChart3,
    title: "Analytics",
    desc: "Track downloads, ratings, and revenue in real-time.",
  },
  {
    icon: Shield,
    title: "IP protection",
    desc: "Your work is watermarked and encrypted. We actively combat piracy.",
  },
];

function SellerPage() {
  return (
    <div className="mx-auto max-w-4xl px-5 pt-28 pb-20">
      <m.header
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="text-center mb-14"
      >
        <div className="af-chip mb-4 mx-auto">
          <span className="w-1.5 h-1.5 rounded-full bg-emerald-400" /> Open for creators
        </div>
        <h1 className="font-display text-4xl md:text-5xl font-bold text-gradient">
          Sell your skins.
        </h1>
        <p className="mt-3 text-muted-foreground max-w-lg mx-auto">
          The KuroStream marketplace is where anime fans discover premium themes. Join hundreds of
          creators earning from their craft.
        </p>
      </m.header>

      <div className="grid md:grid-cols-2 gap-5 mb-12">
        {features.map((f, i) => (
          <m.div
            key={f.title}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 + i * 0.08 }}
            whileHover={{ y: -4 }}
            className="glass-card p-6"
          >
            <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center mb-4">
              <f.icon className="w-5 h-5 text-primary" />
            </div>
            <h3 className="font-display font-semibold text-sm mb-1">{f.title}</h3>
            <p className="text-sm text-muted-foreground leading-relaxed">{f.desc}</p>
          </m.div>
        ))}
      </div>

      <m.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.4 }}
        className="glass-card p-8 text-center"
      >
        <h2 className="font-display text-2xl font-bold mb-3">Ready to start?</h2>
        <p className="text-sm text-muted-foreground mb-6 max-w-md mx-auto">
          Apply to become a verified seller. We'll review your portfolio and get you set up within
          48 hours.
        </p>
        <Button disabled className="h-11 px-6">
          Apply to sell <ArrowRight className="w-4 h-4 ml-1" />
        </Button>
        <p className="text-xs text-muted-foreground mt-3">Seller applications opening Q3 2026.</p>
      </m.div>
    </div>
  );
}
