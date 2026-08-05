import { createFileRoute } from "@tanstack/react-router";
import { z } from "zod";
import { json, preflight, requireUser, getDB, getKV, getEnvVar, checkRateLimit } from "@/lib/kuro-api";
import { getReports, updateReportStatus } from "@/integrations/cloudflare/db";

const reportActionSchema = z.object({
  report_id: z.string().min(1).max(120),
  action: z.enum(["resolve", "dismiss"]),
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

export const Route = createFileRoute("/api/admin/v1/reports")({
  server: {
    handlers: {
      OPTIONS: async () => preflight(),
      GET: async ({ request, env }: { request: Request; env: unknown }) => {
        const kv = getKV(env);
        const auth = await requireAdmin(request, env);
        if ("error" in auth) return auth.error;

        const rate = await checkRateLimit(kv, `admin:reports:${auth.userId}`, 60, 60);
        if (!rate.allowed) return json({ error: "rate_limited", reset: rate.reset }, 429);

        const url = new URL(request.url);
        const status = url.searchParams.get("status") || "pending";

        const db = getDB(env);
        const reports = await getReports(db, status);

        const enriched = await Promise.all(
          reports.map(async (r) => {
            const item = await db
              .prepare(`SELECT id, name, status FROM marketplace_items WHERE id = ?`)
              .bind(r.item_id)
              .first();
            return { ...r, item };
          }),
        );

        return json({
          reports: enriched,
          rate_limit: { remaining: rate.remaining, reset: rate.reset },
        });
      },
    },
  },
});