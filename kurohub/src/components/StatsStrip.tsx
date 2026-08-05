import { m } from "framer-motion";

const stats = [
  { value: "15K+", label: "Downloads" },
  { value: "200+", label: "Skins" },
  { value: "50+", label: "Extensions" },
  { value: "4.9", label: "Rating" },
];

export function StatsStrip() {
  return (
    <section className="relative py-10 border-y border-border/10">
      <div className="mx-auto max-w-6xl px-5">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
          {stats.map((s, i) => (
            <m.div
              key={s.label}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: i * 0.1 }}
              className="text-center"
            >
              <div className="font-display text-2xl md:text-3xl font-bold text-gradient">
                {s.value}
              </div>
              <div className="text-xs text-muted-foreground mt-1">{s.label}</div>
            </m.div>
          ))}
        </div>
      </div>
    </section>
  );
}
