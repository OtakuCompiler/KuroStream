import { Link } from "@tanstack/react-router";
import { Sparkles, ArrowRight, Infinity as InfinityIcon } from "lucide-react";
import { m } from "framer-motion";

export function SkinsPassBanner() {
  return (
    <m.div
      initial={{ opacity: 0, y: 20 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true }}
      className="mb-10 rounded-2xl overflow-hidden relative group"
    >
      <div className="absolute inset-0 bg-gradient-to-r from-primary/20 via-secondary/10 to-primary/20" />
      <div className="absolute inset-0 bg-[url('data:image/svg+xml,%3Csvg width=\'60\' height=\'60\' viewBox=\'0 0 60 60\' xmlns=\'http://www.w3.org/2000/svg\'%3E%3Cg fill=\'none\' fill-rule=\'evenodd\'%3E%3Cg fill=\'%23ffffff\' fill-opacity=\'0.03\'%3E%3Cpath d=\'M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z\'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E')] opacity-50" />
      <div className="relative p-6 md:p-8 flex flex-col md:flex-row items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-primary/20 border border-primary/30 flex items-center justify-center">
            <InfinityIcon className="w-6 h-6 text-primary" />
          </div>
          <div>
            <h3 className="font-display font-semibold text-base">Skins Pass</h3>
            <p className="text-sm text-muted-foreground">
              Unlock every premium skin. One payment, lifetime access.
            </p>
          </div>
        </div>
        <m.div whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.98 }}>
          <Link
            to="/marketplace"
            className="inline-flex items-center gap-2 px-5 py-2.5 rounded-lg bg-primary text-primary-foreground text-sm font-medium hover:bg-primary/90 transition-colors"
          >
            <Sparkles className="w-4 h-4" /> Get Skins Pass <ArrowRight className="w-4 h-4" />
          </Link>
        </m.div>
      </div>
    </m.div>
  );
}
