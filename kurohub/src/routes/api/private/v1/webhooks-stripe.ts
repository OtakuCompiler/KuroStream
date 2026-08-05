import { createFileRoute } from "@tanstack/react-router";
import { json, preflight, getDB } from "@/lib/kuro-api";
import { insertPurchase } from "@/integrations/cloudflare/db";
import type Stripe from "stripe";

export const Route = createFileRoute("/api/private/v1/webhooks-stripe")({
  server: {
    handlers: {
      OPTIONS: async () => preflight(),
      POST: async ({ request, env }: { request: Request; env: unknown }) => {
        const e = env as Record<string, unknown>;
        const stripeSecretKey = e.STRIPE_SECRET_KEY as string | undefined;
        const stripeWebhookSecret = e.STRIPE_WEBHOOK_SECRET as string | undefined;
        if (!stripeSecretKey || !stripeWebhookSecret) return json({ error: "unconfigured" }, 503);

        const StripeClient = (await import("stripe")).default;
        const stripe = new StripeClient(stripeSecretKey, { apiVersion: "2024-06-20" });

        const sig = request.headers.get("stripe-signature");
        if (!sig) return json({ error: "missing_signature" }, 400);

        let event: Stripe.Event;
        try {
          const body = await request.text();
          event = stripe.webhooks.constructEvent(body, sig, stripeWebhookSecret);
        } catch {
          return json({ error: "invalid_signature" }, 400);
        }

        const db = getDB(env);

        if (event.type === "checkout.session.completed") {
          const session = event.data.object as Stripe.Checkout.Session;
          const userId = session.client_reference_id ?? session.metadata?.user_id;
          const itemId = session.metadata?.item_id;
          if (userId && itemId) {
            await insertPurchase(db, userId, itemId, 0);
          }
        }

        return json({ received: true });
      },
    },
  },
});