import { Link } from "@tanstack/react-router";
import { Button } from "@/components/ui/button";
import { BookOpen, Github, Sparkles, Play, ArrowRight, Zap } from "lucide-react";
import { m, useScroll, useTransform } from "framer-motion";
import { useRef, useEffect, useState } from "react";

const HEADLINE = ["The", "anime", "player", "your", "TV", "deserves."];

function ParticleCanvas() {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    let w = (canvas.width = canvas.offsetWidth * window.devicePixelRatio);
    let h = (canvas.height = canvas.offsetHeight * window.devicePixelRatio);
    ctx.scale(window.devicePixelRatio, window.devicePixelRatio);

    const particles: {
      x: number;
      y: number;
      vx: number;
      vy: number;
      size: number;
      alpha: number;
    }[] = [];
    for (let i = 0; i < 60; i++) {
      particles.push({
        x: Math.random() * w,
        y: Math.random() * h,
        vx: (Math.random() - 0.5) * 0.3,
        vy: (Math.random() - 0.5) * 0.3,
        size: Math.random() * 2 + 0.5,
        alpha: Math.random() * 0.5 + 0.1,
      });
    }

    let raf: number;
    const draw = () => {
      ctx.clearRect(0, 0, w, h);
      particles.forEach((p, i) => {
        p.x += p.vx;
        p.y += p.vy;
        if (p.x < 0) p.x = w;
        if (p.x > w) p.x = 0;
        if (p.y < 0) p.y = h;
        if (p.y > h) p.y = 0;

        ctx.beginPath();
        ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
        ctx.fillStyle = `rgba(187, 134, 252, ${p.alpha})`;
        ctx.fill();

        // Connect nearby particles
        for (let j = i + 1; j < particles.length; j++) {
          const dx = particles[j].x - p.x;
          const dy = particles[j].y - p.y;
          const dist = Math.sqrt(dx * dx + dy * dy);
          if (dist < 120) {
            ctx.beginPath();
            ctx.moveTo(p.x, p.y);
            ctx.lineTo(particles[j].x, particles[j].y);
            ctx.strokeStyle = `rgba(187, 134, 252, ${0.08 * (1 - dist / 120)})`;
            ctx.lineWidth = 0.5;
            ctx.stroke();
          }
        }
      });
      raf = requestAnimationFrame(draw);
    };
    draw();

    const onResize = () => {
      w = canvas.width = canvas.offsetWidth * window.devicePixelRatio;
      h = canvas.height = canvas.offsetHeight * window.devicePixelRatio;
      ctx.setTransform(window.devicePixelRatio, 0, 0, window.devicePixelRatio, 0, 0);
    };
    window.addEventListener("resize", onResize);
    return () => {
      cancelAnimationFrame(raf);
      window.removeEventListener("resize", onResize);
    };
  }, []);

  return (
    <canvas
      ref={canvasRef}
      className="absolute inset-0 w-full h-full pointer-events-none"
      style={{ opacity: 0.6 }}
    />
  );
}

export function Hero() {
  const ref = useRef<HTMLElement>(null);
  const { scrollYProgress } = useScroll({
    target: ref,
    offset: ["start start", "end start"],
  });
  const y = useTransform(scrollYProgress, [0, 1], [0, 150]);
  const opacity = useTransform(scrollYProgress, [0, 0.5], [1, 0]);
  const scale = useTransform(scrollYProgress, [0, 0.5], [1, 0.95]);

  return (
    <section ref={ref} className="relative overflow-hidden min-h-[90vh] flex items-center">
      <div className="ambient-layer" aria-hidden />
      <ParticleCanvas />

      <m.div
        className="absolute top-1/4 left-1/2 -translate-x-1/2 w-[700px] h-[700px] rounded-full pointer-events-none"
        style={{
          background: "radial-gradient(circle, oklch(0.55 0.24 295 / 0.08), transparent 70%)",
          y,
        }}
      />

      <m.div
        style={{ opacity, scale }}
        className="relative mx-auto max-w-6xl px-5 pt-32 pb-24 sm:pt-40 sm:pb-32 md:pt-48 md:pb-40 text-center w-full"
      >
        <m.div
          initial={{ opacity: 0, y: 20, scale: 0.95 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
          className="af-chip mx-auto mb-8"
        >
          <span className="relative flex h-2 w-2">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-secondary opacity-75" />
            <span className="relative inline-flex rounded-full h-2 w-2 bg-secondary" />
          </span>
          Open-source · GPL-3.0 · Built for the living room
        </m.div>

        <h1 className="font-display text-5xl sm:text-6xl md:text-7xl lg:text-8xl font-bold tracking-tight leading-[0.95] mx-auto max-w-5xl">
          {HEADLINE.map((word, i) => {
            const emphasize = word === "TV";
            return (
              <m.span
                key={`${word}-${i}`}
                initial={{ opacity: 0, y: 40, rotateX: -40 }}
                animate={{ opacity: 1, y: 0, rotateX: 0 }}
                transition={{
                  duration: 0.7,
                  delay: 0.3 + i * 0.1,
                  ease: [0.22, 1, 0.36, 1],
                }}
                className={`inline-block mr-[0.22em] ${emphasize ? "text-gradient text-glow" : ""}`}
                style={{ transformStyle: "preserve-3d" }}
              >
                {word}
              </m.span>
            );
          })}
        </h1>

        <m.p
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7, delay: 1.0, ease: [0.22, 1, 0.36, 1] }}
          className="mt-8 mx-auto max-w-2xl text-base md:text-lg text-muted-foreground leading-relaxed"
        >
          Plays every format — HLS, DASH, MKV, MP4, RTSP, and your own cloud drives. Runs on every
          screen — TVs, phones, desktops. Zero bloat. Community-powered.
        </m.p>

        <m.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7, delay: 1.15, ease: [0.22, 1, 0.36, 1] }}
          className="mt-10 flex flex-wrap justify-center gap-3"
        >
          <m.div whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.98 }}>
            <Button
              size="lg"
              disabled
              className="h-13 px-8 text-base bg-primary/25 text-primary-foreground/90 border border-primary/40 cursor-not-allowed animate-pulse-glow"
            >
              <Sparkles className="w-4 h-4" /> APK · Work in progress
            </Button>
          </m.div>
          <m.div whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.98 }}>
            <Button
              asChild
              size="lg"
              variant="outline"
              className="h-13 px-7 text-base af-panel border-0 hover:shadow-[var(--glow-primary)] transition-shadow"
            >
              <Link to="/setup">
                <BookOpen className="w-4 h-4" /> Setup guide
              </Link>
            </Button>
          </m.div>
          <m.div whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.98 }}>
            <Button asChild size="lg" variant="ghost" className="h-13 px-6 text-base group">
              <a
                href="https://github.com/OtakuCompiler/KuroStream"
                target="_blank"
                rel="noreferrer"
              >
                <Github className="w-4 h-4 group-hover:rotate-12 transition-transform" /> Star on
                GitHub
              </a>
            </Button>
          </m.div>
        </m.div>

        <m.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 0.7, delay: 1.4 }}
          className="mt-14 flex flex-wrap justify-center gap-x-6 gap-y-3 text-xs text-muted-foreground"
        >
          {["ExoPlayer + VLC", "Android TV 5+", "Zero telemetry", "4K HDR"].map((tag) => (
            <span key={tag} className="flex items-center gap-1.5">
              <Zap className="w-3 h-3 text-primary/60" /> {tag}
            </span>
          ))}
        </m.div>

        {/* Scroll indicator */}
        <m.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 2, duration: 1 }}
          className="absolute bottom-8 left-1/2 -translate-x-1/2"
        >
          <m.div
            animate={{ y: [0, 8, 0] }}
            transition={{ duration: 2, repeat: Infinity, ease: "easeInOut" }}
            className="w-6 h-10 rounded-full border-2 border-muted-foreground/30 flex items-start justify-center p-1.5"
          >
            <m.div
              animate={{ opacity: [1, 0.3, 1], y: [0, 8, 0] }}
              transition={{ duration: 2, repeat: Infinity, ease: "easeInOut" }}
              className="w-1 h-2 rounded-full bg-muted-foreground/60"
            />
          </m.div>
        </m.div>
      </m.div>
    </section>
  );
}
