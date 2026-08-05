import { createFileRoute } from "@tanstack/react-router";
import { z } from "zod";
import { json, preflight, requireUser, getDB, getKV, checkRateLimit } from "@/lib/kuro-api";
import { getItemById, getPurchasedItems, setActiveSkin } from "@/integrations/cloudflare/db";

const SKINS_PASS_ID = "skins_pass";
const bodySchema = z.object({ item_id: z.string().min(1).max(120) });

export const Route = createFileRoute("/api/public/v1/active-skin")({
  server: {
    handlers: {
      OPTIONS: async () => preflight(),
      POST: async ({ request, env }: { request: Request; env: unknown }) => {
        const auth = await requireUser(request, env);
        if ("error" in auth) return auth.error;

        const kv = getKV(env);
        const rate = await checkRateLimit(kv, `active_skin:${auth.userId}`, 30, 60);
        if (!rate.allowed) return json({ error: "rate_limited", reset: rate.reset }, 429);

        const db = getDB(env);
        const parsed = bodySchema.safeParse(await request.json().catch(() => null));
        if (!parsed.success) return json({ error: "invalid_body" }, 400);
        const itemId = parsed.data.item_id;

        const [item, ownedIds] = await Promise.all([
          getItemById(db, itemId),
          getPurchasedItems(db, auth.userId),
        ]);
        if (!item) return json({ error: "unknown_item" }, 404);

        const owned = new Set(ownedIds);
        const entitled =
          owned.has(itemId) ||
          Number(item.price) === 0 ||
          (owned.has(SKINS_PASS_ID) && item.category === "skin");
        if (!entitled) return json({ error: "not_entitled" }, 403);

        await setActiveSkin(db, auth.userId, itemId);
        return json({ ok: true, active_skin_id: itemId });
      },
    },
  },
});