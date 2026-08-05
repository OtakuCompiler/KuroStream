import { createFileRoute } from "@tanstack/react-router";
import { z } from "zod";
import { json, preflight, requireUser, getDB, getKV, getEnvVar } from "@/lib/kuro-api";
import { getPurchases, getItemById } from "@/integrations/cloudflare/db";

const querySchema = z.object({ user_id: z.string().min(1).max(120) });

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

export const Route = createFileRoute("/api/admin/v1/entitlements")({
  server: {
    handlers: {
      OPTIONS: async () => preflight(),
      GET: async ({ request, env }: { request: Request; env: unknown }) => {
        const kv = getKV(env);
        const auth = await requireAdmin(request, env);
        if ("error" in auth) return auth.error;

        const url = new URL(request.url);
        const parsed = querySchema.safeParse({ user_id: url.searchParams.get("user_id") ?? "" });
        if (!parsed.success) return json({ error: "invalid_query" }, 400);

        const db = getDB(env);
        const purchases = await getPurchases(db, parsed.data.user_id);
        const items = await Promise.all(purchases.map((p) => getItemById(db, p.item_id)));
        const entitlements = purchases.map((p, i) => ({
          item_id: p.item_id,
          amount: p.amount,
          status: p.status,
          created_at: p.created_at,
          item: items[i]
            ? {
                id: items[i].id,
                name: items[i].name,
                price: items[i].price,
                category: items[i].category,
              }
            : null,
        }));

        return json({
          user_id: parsed.data.user_id,
          entitlements,
          rate_limit: { remaining: 0, reset: 0 },
        });
      },
    },
  },
});