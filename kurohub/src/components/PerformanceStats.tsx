import { useEffect, useRef, useState } from "react";
import { Cpu, Gauge, ShieldCheck, Radio } from "lucide-react";
import { m, useInView } from "framer-motion";

type Stat = {
  icon: typeof Cpu;
  label: string;
  value: string;
  suffix?: string;
  detail: string;
  numeric?: number;
};

const stats: Stat[] = [
  {
    icon: Gauge,
    label: "Cold start",
    value: "1.4",
    suffix: "s",
    detail: "on 1 GB Fire Stick",
    numeric: 1.4,
  },
  {
    icon: Cpu,
    label: "Idle RAM",
    value: "180",
    suffix: "MB",
    detail: "extension-driven, no bloat",
    numeric: 180,
  },
  {
    icon: Radio,
    label: "Formats",
    value: "20",
    suffix: "+",
    detail: "HLS · DASH · MKV · cloud",
    numeric: 20,
  },
  {
    icon: ShieldCheck,
    label: "Telemetry",
    value: "0",
    detail: "no trackers, no analytics",
    numeric: 0,
  },
];

function AnimatedCounter({ target, suffix = "" }: { target: number; suffix?: string }) {
  const [count, setCount] = useState(0);
  const ref = useRef<HTMLSpanElement>(null);
  const isInView = useInView(ref, { once: true, margin: "-50px" });

  useEffect(() => {
    if (!isInView) return;
    const start = 0;
    const duration = 2000;
    const startTime = performance.now();
    const animate = (now: number) => {
      const elapsed = now - startTime;
      const progress = Math.min(elapsed / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      setCount(Math.floor(eased * target));
      if (progress < 1) requestAnimationFrame(animate);
    };
    requestAnimationFrame(animate);
  }, [isInView, target]);

  return (
    <span ref={ref} className="counter-tick">
      {count}
      {suffix}
    </span>
  );
}

export function PerformanceStats() {
  return (
    <section className="relative py-24 md:py-32 content-visibility-auto">
      <div className="mx-auto max-w-6xl px-5">
        <header className="max-w-2xl mb-14">
          <m.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="af-chip mb-4"
          >
            <span className="w-1.5 h-1.5 rounded-full bg-primary" /> Performance
          </m.div>
          <m.h2
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: 0.1 }}
            className="font-display text-4xl md:text-5xl font-bold tracking-tight"
          >
            Fast where it counts. <span className="text-gradient">Silent everywhere else.</span>
          </m.h2>
          <m.p
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: 0.2 }}
            className="mt-3 text-muted-foreground"
          >
            Numbers we measure on the oldest hardware we still support. If your box can boot Android
            5, it can run KuroStream.
          </m.p>
        </header>

        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 md:gap-5">
          {stats.map((s, i) => (
            <m.div
              key={s.label}
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: i * 0.1, duration: 0.5 }}
              whileHover={{ scale: 1.03, y: -4 }}
              className="glow-border af-panel p-5 md:p-6 flex flex-col justify-between cursor-default group"
            >
              <div className="flex items-center justify-between mb-4">
                <s.icon className="w-5 h-5 text-primary/70 group-hover:text-primary transition-colors" />
                <span className="text-[10px] uppercase tracking-wider text-muted-foreground/50">
                  {s.label}
                </span>
              </div>
              <div>
                <div className="font-display text-3xl md:text-4xl font-bold text-gradient">
                  {s.numeric !== undefined ? (
                    <AnimatedCounter target={s.numeric} suffix={s.suffix || ""} />
                  ) : (
                    s.value
                  )}
                </div>
                <p className="text-xs text-muted-foreground mt-2">{s.detail}</p>
              </div>
            </m.div>
          ))}
        </div>
      </div>
    </section>
  );
}
