import { useRef } from "react";
import { m, useScroll, useTransform } from "framer-motion";
import desktop from "@/assets/mockup-desktop.jpg";
import phone from "@/assets/mockup-phone.png";
import tv from "@/assets/mockup-tv.jpg";

export function DeviceShowcase() {
  const ref = useRef<HTMLElement>(null);
  const { scrollYProgress } = useScroll({
    target: ref,
    offset: ["start end", "end start"],
  });

  const y1 = useTransform(scrollYProgress, [0, 1], [100, -100]);
  const y2 = useTransform(scrollYProgress, [0, 1], [60, -60]);
  const y3 = useTransform(scrollYProgress, [0, 1], [40, -40]);
  const scale = useTransform(scrollYProgress, [0, 0.5, 1], [0.9, 1, 0.9]);
  const opacity = useTransform(scrollYProgress, [0, 0.2, 0.8, 1], [0, 1, 1, 0]);

  return (
    <section ref={ref} className="relative py-24 md:py-32 overflow-hidden content-visibility-auto">
      <div className="mx-auto max-w-6xl px-5">
        <m.header
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="max-w-2xl mb-16 text-center mx-auto"
        >
          <div className="af-chip mb-4 mx-auto">
            <span className="w-1.5 h-1.5 rounded-full bg-secondary" /> Devices
          </div>
          <h2 className="font-display text-4xl md:text-5xl font-bold tracking-tight">
            One app. <span className="text-gradient">Every screen.</span>
          </h2>
          <p className="mt-3 text-muted-foreground">
            Start on your TV, continue on your phone, finish on your laptop. Seamless.
          </p>
        </m.header>

        <m.div
          style={{ opacity, scale }}
          className="relative flex items-center justify-center gap-4 md:gap-8 perspective-[1200px]"
        >
          <m.div
            style={{ y: y1 }}
            className="w-1/4 rounded-xl overflow-hidden shadow-2xl ring-1 ring-white/10"
          >
            <img src={phone} alt="Phone mockup" className="w-full h-auto" loading="lazy" />
          </m.div>
          <m.div
            style={{ y: y2 }}
            className="w-2/5 rounded-2xl overflow-hidden shadow-2xl ring-1 ring-white/10 z-10"
          >
            <img src={tv} alt="TV mockup" className="w-full h-auto" loading="lazy" />
          </m.div>
          <m.div
            style={{ y: y3 }}
            className="w-1/3 rounded-xl overflow-hidden shadow-2xl ring-1 ring-white/10"
          >
            <img src={desktop} alt="Desktop mockup" className="w-full h-auto" loading="lazy" />
          </m.div>
        </m.div>
      </div>
    </section>
  );
}
