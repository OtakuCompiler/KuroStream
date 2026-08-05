import { createFileRoute } from "@tanstack/react-router";
import { z } from "zod";
import { json, preflight, requireUser, getDB, getKV, checkRateLimit } from "@/lib/kuro-api";

const reportSchema = z.object({
  item_id: z.string().min(1).max(120),
  reason: z.enum(["piracy", "copyright", "malware", "misleading", "spam", "other"]),
  details: z.string().max(2000).optional(),
});

export const Route = createFileRoute("/api/public/v1/report")({
  server: {
    handlers: {
      OPTIONS: async () => preflight(),
      POST: async ({ request, env }: any) => {
        const auth = await requireUser(request, env);
        if ("error" in auth) return auth.error;

        const kv = getKV(env);
        const rate = await checkRateLimit(kv, `report:${auth.userId}`, 10, 3600);
        if (!rate.allowed) return json({ error: "rate_limited", reset: rate.reset }, 429);

        const db = getDB(env);
        const parsed = reportSchema.safeParse(await request.json().catch(() => null));
        if (!parsed.success)
          return json({ error: "invalid_body", details: parsed.error.flatten() }, 400);

        const { item_id, reason, details } = parsed.data;

        const item = await db
          .prepare(`SELECT id, status FROM marketplace_items WHERE id = ?`)
          .bind(item_id)
          .first<{ id: string; status: string }>();
        if (!item) return json({ error: "item_not_found" }, 404);

        const existing = await db
          .prepare(`SELECT id FROM reports WHERE item_id = ? AND reporter_id = ?`)
          .bind(item_id, auth.userId)
          .first();
        if (existing) return json({ error: "already_reported" }, 409);

        const now = new Date().toISOString();
        await db
          .prepare(
            `
          INSERT INTO reports (item_id, reporter_id, reason, details, created_at, status)
          VALUES (?, ?, ?, ?, ?, 'pending')
        `,
          )
          .bind(item_id, auth.userId, reason, details || null, now)
          .run();

        const reportCount = await db
          .prepare(`SELECT COUNT(*) as count FROM reports WHERE item_id = ? AND status = 'pending'`)
          .bind(item_id)
          .first<{ count: number }>();
        if (reportCount && reportCount.count >= 3) {
          await db
            .prepare(`UPDATE marketplace_items SET status = 'hidden', updated_at = ? WHERE id = ?`)
            .bind(now, item_id)
            .run();
        }

        return json({ ok: true, report_count: reportCount?.count || 1 });
      },
    },
  },
});