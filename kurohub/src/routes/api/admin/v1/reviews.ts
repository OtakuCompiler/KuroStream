import { createFileRoute } from "@tanstack/react-router";
import { z } from "zod";
import { json, preflight, requireUser, getDB, getKV, getEnvVar, checkRateLimit } from "@/lib/kuro-api";
import { getPendingReviews, getReviewById, updateReviewStatus } from "@/integrations/cloudflare/db";

async function requireAdmin(
  request: Request,
  env: unknown,
): Promise<{ userId: string } | { error: Response }> {
  const auth = await requireUser(request, env);
  if ("error" in auth) return { error: auth.error };

  const adminKey = request.headers.get("x-admin-key");
  const expectedKey = getEnvVar(env, "ADMIN_KEY");
  if (!adminKey || adminKey !== expectedKey) {
    return { error: json({ error: "admin_required" }, 403) };
  }

  return { userId: auth.userId };
}

export const Route = createFileRoute("/api/admin/v1/reviews")({
  server: {
    handlers: {
      OPTIONS: async () => preflight(),
      GET: async ({ request, env }: { request: Request; env: unknown }) => {
        const kv = getKV(env);
        const auth = await requireAdmin(request, env);
        if ("error" in auth) return auth.error;

        const rate = await checkRateLimit(kv, `admin:reviews:${auth.userId}`, 60, 60);
        if (!rate.allowed) return json({ error: "rate_limited", reset: rate.reset }, 429);

        const db = getDB(env);
        const reviews = await getPendingReviews(db);

        const enriched = await Promise.all(
          reviews.map(async (r) => {
            const item = await db
              .prepare(`SELECT * FROM marketplace_items WHERE id = ?`)
              .bind(r.item_id)
              .first();
            return { ...r, item };
          }),
        );

        return json({
          reviews: enriched,
          rate_limit: { remaining: rate.remaining, reset: rate.reset },
        });
      },
    },
  },
});