import { useState, useEffect, useCallback } from "react";
import { Quote, ChevronLeft, ChevronRight, Star } from "lucide-react";
import { m, AnimatePresence } from "framer-motion";

const testimonials = [
  {
    text: "Finally a TV app that doesn't feel like a mobile port. The D-pad navigation is buttery smooth and the skin system is genuinely fun.",
    author: "Alex Chen",
    role: "Android TV enthusiast",
    rating: 5,
  },
  {
    text: "I replaced three different streaming apps with KuroStream. The extension system means I never have to hunt for sources again.",
    author: "Sarah Kim",
    role: "Cord-cutter since 2019",
    rating: 5,
  },
  {
    text: "The zero telemetry promise sold me. Fast, private, and the community keeps improving it. This is what open source should be.",
    author: "Marcus Webb",
    role: "Privacy advocate",
    rating: 5,
  },
];

export function Testimonials() {
  const [idx, setIdx] = useState(0);
  const [direction, setDirection] = useState(0);

  const next = useCallback(() => {
    setDirection(1);
    setIdx((i) => (i + 1) % testimonials.length);
  }, []);

  const prev = useCallback(() => {
    setDirection(-1);
    setIdx((i) => (i - 1 + testimonials.length) % testimonials.length);
  }, []);

  useEffect(() => {
    const timer = setInterval(next, 6000);
    return () => clearInterval(timer);
  }, [next]);

  const variants = {
    enter: (d: number) => ({ x: d > 0 ? 300 : -300, opacity: 0 }),
    center: { x: 0, opacity: 1 },
    exit: (d: number) => ({ x: d > 0 ? -300 : 300, opacity: 0 }),
  };

  return (
    <section className="relative py-24 md:py-32 content-visibility-auto">
      <div className="mx-auto max-w-4xl px-5">
        <m.header
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="text-center mb-14"
        >
          <div className="af-chip mb-4 mx-auto">
            <span className="w-1.5 h-1.5 rounded-full bg-primary" /> Community
          </div>
          <h2 className="font-display text-4xl md:text-5xl font-bold tracking-tight">
            Loved by <span className="text-gradient">fans</span>.
          </h2>
        </m.header>

        <div className="relative">
          <div className="overflow-hidden min-h-[200px]">
            <AnimatePresence mode="wait" custom={direction}>
              <m.div
                key={idx}
                custom={direction}
                variants={variants}
                initial="enter"
                animate="center"
                exit="exit"
                transition={{ duration: 0.4, ease: [0.22, 1, 0.36, 1] }}
                className="glass-card p-8 md:p-10 text-center"
              >
                <Quote className="w-8 h-8 text-primary/30 mx-auto mb-4" />
                <p className="text-lg md:text-xl text-foreground/90 leading-relaxed mb-6 max-w-2xl mx-auto">
                  "{testimonials[idx]!.text}"
                </p>
                <div className="flex items-center justify-center gap-1 mb-3">
                  {Array.from({ length: testimonials[idx]!.rating }).map((_, i) => (
                    <Star key={i} className="w-4 h-4 text-amber-400 fill-amber-400" />
                  ))}
                </div>
                <div className="font-display font-semibold text-sm">{testimonials[idx]!.author}</div>
                <div className="text-xs text-muted-foreground">{testimonials[idx]!.role}</div>
              </m.div>
            </AnimatePresence>
          </div>

          <div className="flex items-center justify-center gap-3 mt-6">
            <button
              onClick={prev}
              className="w-10 h-10 rounded-full bg-surface-2 border border-stroke-soft flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-white/5 transition-all"
              aria-label="Previous testimonial"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <div className="flex gap-1.5">
              {testimonials.map((_, i) => (
                <button
                  key={i}
                  onClick={() => {
                    setDirection(i > idx ? 1 : -1);
                    setIdx(i);
                  }}
                  className={`h-1.5 rounded-full transition-all duration-300 ${i === idx ? "w-6 bg-primary" : "w-1.5 bg-muted-foreground/30 hover:bg-muted-foreground/50"}`}
                  aria-label={`Go to testimonial ${i + 1}`}
                />
              ))}
            </div>
            <button
              onClick={next}
              className="w-10 h-10 rounded-full bg-surface-2 border border-stroke-soft flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-white/5 transition-all"
              aria-label="Next testimonial"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>
    </section>
  );
}
