import { createFileRoute } from "@tanstack/react-router";
import { m } from "framer-motion";
import { LegalBlock } from "@/components/LegalBlock";

export const Route = createFileRoute("/legal")({
  head: () => ({
    meta: [
      { title: "Legal — Kuro Stream" },
      {
        name: "description",
        content: "Privacy policy, terms of service, and license information for KuroStream.",
      },
    ],
  }),
  component: LegalPage,
});

function LegalPage() {
  return (
    <div className="mx-auto max-w-4xl px-5 pt-28 pb-20">
      <m.header
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="text-center mb-10"
      >
        <h1 className="font-display text-4xl font-bold text-gradient">Legal.</h1>
        <p className="mt-2 text-muted-foreground">Transparency and trust.</p>
      </m.header>
      <LegalBlock />
    </div>
  );
}
