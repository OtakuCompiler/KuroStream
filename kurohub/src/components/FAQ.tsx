import { useState } from "react";
import { HelpCircle, ChevronDown } from "lucide-react";
import { m, AnimatePresence } from "framer-motion";

const faqs = [
  {
    q: "Is KuroStream free?",
    a: "Yes. The app is open-source under GPL-3.0 and completely free to use. Premium skins in the marketplace are optional — free themes are included.",
  },
  {
    q: "What devices are supported?",
    a: "Android TV, Google TV, Fire TV, and NVIDIA Shield are fully supported today. Mobile and desktop builds are in active development.",
  },
  {
    q: "How do extensions work?",
    a: "Extensions are small adapters, built on our open plugin SDK, that add metadata enrichment, subtitle sources, watch-list sync, and library tools to KuroStream. Install them from the marketplace and they auto-update.",
  },
  {
    q: "Is my data private?",
    a: "Absolutely. We collect zero telemetry. Your watch history, preferences, and account data stay on your device or in your own Firebase project.",
  },
];

function FAQItem({ q, a, i }: { q: string; a: string; i: number }) {
  const [open, setOpen] = useState(false);
  return (
    <m.div
      initial={{ opacity: 0, y: 20 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true }}
      transition={{ delay: i * 0.08 }}
      className="border-b border-border/20 last:border-0"
    >
      <button
        onClick={() => setOpen(!open)}
        className="w-full flex items-center justify-between py-5 text-left group"
      >
        <span className="font-display font-medium text-sm pr-4 group-hover:text-primary transition-colors">
          {q}
        </span>
        <m.div animate={{ rotate: open ? 180 : 0 }} transition={{ duration: 0.3 }}>
          <ChevronDown className="w-4 h-4 text-muted-foreground flex-shrink-0" />
        </m.div>
      </button>
      <AnimatePresence>
        {open && (
          <m.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: "auto", opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.3, ease: [0.22, 1, 0.36, 1] }}
            className="overflow-hidden"
          >
            <p className="text-sm text-muted-foreground leading-relaxed pb-5">{a}</p>
          </m.div>
        )}
      </AnimatePresence>
    </m.div>
  );
}

export function FAQ() {
  return (
    <section className="relative py-24 md:py-32 content-visibility-auto">
      <div className="mx-auto max-w-3xl px-5">
        <m.header
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="text-center mb-14"
        >
          <div className="af-chip mb-4 mx-auto">
            <HelpCircle className="w-3 h-3 text-primary" /> FAQ
          </div>
          <h2 className="font-display text-4xl md:text-5xl font-bold tracking-tight">
            Questions? <span className="text-gradient">Answered.</span>
          </h2>
        </m.header>

        <div className="glass-card p-6 md:p-8">
          {faqs.map((f, i) => (
            <FAQItem key={i} q={f.q} a={f.a} i={i} />
          ))}
        </div>
      </div>
    </section>
  );
}
