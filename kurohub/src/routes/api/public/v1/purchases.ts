import type { D1Database } from "@/integrations/cloudflare/types";
import { createFileRoute } from "@tanstack/react-router";
import { z } from "zod";
import { json, preflight, requireUser, getDB, getKV, checkRateLimit } from "@/lib/kuro-api";
import { getPurchases, insertPurchase, getItemById } from "@/integrations/cloudflare/db";

const SKINS_PASS_ID = "skins_pass";
const bodySchema = z.object({ item_id: z.string().min(1).max(120) });

async function getUserEntitlements(db: D1Database, userId: string): Promise<string[]> {
  const purchases = await getPurchases(db, userId);
  return purchases.map((p) => p.item_id);
}

async function hasEntitlement(db: D1Database, userId: string, itemId: string): Promise<boolean> {
  const owned = await getUserEntitlements(db, userId);
  if (owned.includes(itemId)) return true;
  if (itemId === SKINS_PASS_ID) return false;
  const item = await getItemById(db, itemId);
  if (!item) return false;
  if (Number(item.price) > 0 && owned.includes(SKINS_PASS_ID) && item.category === "skin")
    return true;
  return false;
}

export const Route = createFileRoute("/api/public/v1/purchases")({
  server: {
    handlers: {
      OPTIONS: async () => preflight(),
      GET: async ({ request, env }: { request: Request; env: unknown }) => {
        const auth = await requireUser(request, env);
        if ("error" in auth) return auth.error;

        const kv = getKV(env);
        const rate = await checkRateLimit(kv, `purchases:${auth.userId}`, 60, 60);
        if (!rate.allowed) return json({ error: "rate_limited", reset: rate.reset }, 429);

        const db = getDB(env);
        const purchases = await getPurchases(db, auth.userId);
        return json({ purchases, rate_limit: { remaining: rate.remaining, reset: rate.reset } });
      },
      POST: async ({ request, env }: { request: Request; env: unknown }) => {
        const auth = await requireUser(request, env);
        if ("error" in auth) return auth.error;

        const kv = getKV(env);
        const rate = await checkRateLimit(kv, `purchases:write:${auth.userId}`, 10, 60);
        if (!rate.allowed) return json({ error: "rate_limited", reset: rate.reset }, 429);

        const db = getDB(env);
        const parsed = bodySchema.safeParse(await request.json().catch(() => null));
        if (!parsed.success) return json({ error: "invalid_body" }, 400);

        const itemId = parsed.data.item_id;

        if (await hasEntitlement(db, auth.userId, itemId)) {
          return json({ ok: true, item_id: itemId, already_owned: true });
        }

        const item = await getItemById(db, itemId);
        if (!item) return json({ error: "unknown_item" }, 404);
        if (Number(item.price) > 0) return json({ error: "payment_required" }, 402);

        await insertPurchase(db, auth.userId, item.id, 0);
        return json({ ok: true, item_id: item.id });
      },
    },
  },
});