import { createFileRoute } from "@tanstack/react-router";
import { json, preflight, requireUser, getDB, getKV, checkRateLimit } from "@/lib/kuro-api";
import { getProfile, getPurchases, getActiveSkin } from "@/integrations/cloudflare/db";

const SKINS_PASS_ID = "skins_pass";

export const Route = createFileRoute("/api/public/v1/me")({
  server: {
    handlers: {
      OPTIONS: async () => preflight(),
      GET: async ({ request, env }: { request: Request; env: unknown }) => {
        const auth = await requireUser(request, env);
        if ("error" in auth) return auth.error;

        const kv = getKV(env);
        const rate = await checkRateLimit(kv, `me:${auth.userId}`, 60, 60);
        if (!rate.allowed) {
          return json({ error: "rate_limited", reset: rate.reset }, 429);
        }

        const db = getDB(env);
        const [profile, purchases, active] = await Promise.all([
          getProfile(db, auth.userId),
          getPurchases(db, auth.userId),
          getActiveSkin(db, auth.userId),
        ]);

        const owned = purchases.map((p) => p.item_id);
        return json({
          user: {
            id: auth.userId,
            display_name: profile?.display_name ?? null,
            avatar_url: profile?.avatar_url ?? null,
          },
          entitlements: {
            owned_item_ids: owned,
            has_skins_pass: owned.includes(SKINS_PASS_ID),
            active_skin_id: active?.item_id ?? null,
          },
          purchases,
          rate_limit: { remaining: rate.remaining, reset: rate.reset },
        });
      },
    },
  },
});