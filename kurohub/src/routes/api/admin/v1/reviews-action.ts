import type { KVNamespace } from "@/integrations/cloudflare/types";
import { createFileRoute } from "@tanstack/react-router";
import { z } from "zod";
import { json, preflight, requireUser, getDB, getKV, checkRateLimit } from "@/lib/kuro-api";
import { updateReviewStatus } from "@/integrations/cloudflare/db";

const reviewActionSchema = z.object({
  item_id: z.string().min(1).max(120),
  action: z.enum(["approve", "reject"]),
  rejection_reason: z.string().max(1000).optional(),
});

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

export const Route = createFileRoute("/api/admin/v1/reviews-action")({
  server: {
    handlers: {
      OPTIONS: async () => preflight(),
      POST: async ({ request, env }: { request: Request; env: unknown }) => {
        const kv = getKV(env);
        const auth = await requireAdmin(request, env);
        if ("error" in auth) return auth.error;

        const rate = await checkRateLimit(kv, `admin:review_action:${auth.userId}`, 30, 60);
        if (!rate.allowed) return json({ error: "rate_limited", reset: rate.reset }, 429);

        const db = getDB(env);
        const parsed = reviewActionSchema.safeParse(await request.json().catch(() => null));
        if (!parsed.success)
          return json({ error: "invalid_body", details: parsed.error.flatten() }, 400);

        const { item_id, action, rejection_reason } = parsed.data;

        const status = action === "approve" ? "approved" : "rejected";
        await updateReviewStatus(db, item_id, status, auth.userId, rejection_reason);

        return json({ ok: true, item_id, action });
      },
    },
  },
});

function getEnvVar(env: unknown, key: string): string | undefined {
  const e = env as Record<string, unknown>;
  return e[key] as string | undefined;
}