import { createFileRoute } from "@tanstack/react-router";
import { m } from "framer-motion";
import { Hero } from "@/components/Hero";
import { FormatsMarquee } from "@/components/FormatsMarquee";
import { FeaturedSkins } from "@/components/FeaturedSkins";
import { BentoFeatures } from "@/components/BentoFeatures";
import { PlatformsGrid } from "@/components/PlatformsGrid";
import { PerformanceStats } from "@/components/PerformanceStats";
import { HowItWorks } from "@/components/HowItWorks";
import { DeviceShowcase } from "@/components/DeviceShowcase";
import { Testimonials } from "@/components/Testimonials";
import { FAQ } from "@/components/FAQ";
import { DeveloperSection } from "@/components/DeveloperSection";
import { StatsStrip } from "@/components/StatsStrip";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Kuro Stream — The anime player your TV deserves" },
      {
        name: "description",
        content:
          "Open-source Android TV streaming app. Plays every format, runs on every screen, zero bloat.",
      },
      { property: "og:title", content: "Kuro Stream — The anime player your TV deserves" },
      {
        property: "og:description",
        content:
          "Open-source Android TV streaming app with premium skins, extensions, and zero telemetry.",
      },
    ],
  }),
  component: HomePage,
});

const sectionVariants = {
  hidden: { opacity: 0, y: 40 },
  visible: { opacity: 1, y: 0 },
};

function HomePage() {
  return (
    <>
      <Hero />
      <m.div
        initial="hidden"
        whileInView="visible"
        viewport={{ once: true, margin: "-100px" }}
        variants={sectionVariants}
        transition={{ duration: 0.6 }}
      >
        <FormatsMarquee />
      </m.div>
      <m.div
        initial="hidden"
        whileInView="visible"
        viewport={{ once: true, margin: "-100px" }}
        variants={sectionVariants}
        transition={{ duration: 0.6, delay: 0.1 }}
      >
        <StatsStrip />
      </m.div>
      <m.div
        initial="hidden"
        whileInView="visible"
        viewport={{ once: true, margin: "-100px" }}
        variants={sectionVariants}
        transition={{ duration: 0.6, delay: 0.1 }}
      >
        <FeaturedSkins />
      </m.div>
      <m.div
        initial="hidden"
        whileInView="visible"
        viewport={{ once: true, margin: "-100px" }}
        variants={sectionVariants}
        transition={{ duration: 0.6, delay: 0.1 }}
      >
        <BentoFeatures />
      </m.div>
      <m.div
        initial="hidden"
        whileInView="visible"
        viewport={{ once: true, margin: "-100px" }}
        variants={sectionVariants}
        transition={{ duration: 0.6, delay: 0.1 }}
      >
        <PlatformsGrid />
      </m.div>
      <m.div
        initial="hidden"
        whileInView="visible"
        viewport={{ once: true, margin: "-100px" }}
        variants={sectionVariants}
        transition={{ duration: 0.6, delay: 0.1 }}
      >
        <PerformanceStats />
      </m.div>
      <m.div
        initial="hidden"
        whileInView="visible"
        viewport={{ once: true, margin: "-100px" }}
        variants={sectionVariants}
        transition={{ duration: 0.6, delay: 0.1 }}
      >
        <HowItWorks />
      </m.div>
      <m.div
        initial="hidden"
        whileInView="visible"
        viewport={{ once: true, margin: "-100px" }}
        variants={sectionVariants}
        transition={{ duration: 0.6, delay: 0.1 }}
      >
        <DeviceShowcase />
      </m.div>
      <m.div
        initial="hidden"
        whileInView="visible"
        viewport={{ once: true, margin: "-100px" }}
        variants={sectionVariants}
        transition={{ duration: 0.6, delay: 0.1 }}
      >
        <Testimonials />
      </m.div>
      <m.div
        initial="hidden"
        whileInView="visible"
        viewport={{ once: true, margin: "-100px" }}
        variants={sectionVariants}
        transition={{ duration: 0.6, delay: 0.1 }}
      >
        <FAQ />
      </m.div>
      <m.div
        initial="hidden"
        whileInView="visible"
        viewport={{ once: true, margin: "-100px" }}
        variants={sectionVariants}
        transition={{ duration: 0.6, delay: 0.1 }}
      >
        <DeveloperSection />
      </m.div>
    </>
  );
}
