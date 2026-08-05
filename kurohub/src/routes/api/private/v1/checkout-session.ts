import { createFileRoute } from "@tanstack/react-router";
import { z } from "zod";
import { json, preflight, requireUser, getDB, getKV, getEnvVar, checkRateLimit } from "@/lib/kuro-api";
import { getItemById } from "@/integrations/cloudflare/db";

const bodySchema = z.object({ item_id: z.string().min(1).max(120) });

export const Route = createFileRoute("/api/private/v1/checkout-session")({
  server: {
    handlers: {
      OPTIONS: async () => preflight(),
      POST: async ({ request, env }: any) => {
        const auth = await requireUser(request, env);
        if ("error" in auth) return auth.error;

        const kv = getKV(env);
        const rate = await checkRateLimit(kv, `checkout:${auth.userId}`, 10, 60);
        if (!rate.allowed) return json({ error: "rate_limited", reset: rate.reset }, 429);

        const parsed = bodySchema.safeParse(await request.json().catch(() => null));
        if (!parsed.success) return json({ error: "invalid_body" }, 400);

        const db = getDB(env);
        const item = await getItemById(db, parsed.data.item_id);
        if (!item) return json({ error: "unknown_item" }, 404);
        if (Number(item.price) <= 0) return json({ error: "not_payable" }, 400);

        const stripeSecretKey = getEnvVar(env, "STRIPE_SECRET_KEY");
        if (!stripeSecretKey) return json({ error: "payment_system_unavailable" }, 503);

        const Stripe = (await import("stripe")).default;
        const stripe = new Stripe(stripeSecretKey, { apiVersion: "2024-06-20" });

        const session = await stripe.checkout.sessions.create({
          mode: "payment",
          payment_method_types: ["card"],
          line_items: [
            {
              price_data: {
                currency: "usd",
                product_data: { name: item.name, description: item.description ?? undefined },
                unit_amount: Math.round(Number(item.price) * 100),
              },
              quantity: 1,
            },
          ],
          success_url: `${new URL(request.url).origin}/marketplace?purchase=success`,
          cancel_url: `${new URL(request.url).origin}/marketplace?purchase=cancelled`,
          metadata: { user_id: auth.userId, item_id: item.id },
          client_reference_id: auth.userId,
        });

        return json({ session_id: session.id, url: session.url });
      },
    },
  },
});