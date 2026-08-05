import { m } from "framer-motion";
import type { LucideIcon } from "lucide-react";

interface Props {
  features: { icon: LucideIcon; title: string; body: string }[];
}

export function FeatureGrid({ features }: Props) {
  return (
    <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-5">
      {features.map((f, i) => (
        <m.div
          key={f.title}
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ delay: i * 0.08 }}
          whileHover={{ y: -4 }}
          className="glow-border glass-card p-6 group cursor-default"
        >
          <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center mb-4 group-hover:bg-primary/20 transition-colors">
            <f.icon className="w-5 h-5 text-primary" />
          </div>
          <h3 className="font-display font-semibold text-sm mb-2">{f.title}</h3>
          <p className="text-sm text-muted-foreground leading-relaxed">{f.body}</p>
        </m.div>
      ))}
    </div>
  );
}
