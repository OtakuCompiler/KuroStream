import { createFileRoute } from "@tanstack/react-router";
import { json, preflight, requireUser, getDB, getKV, checkRateLimit } from "@/lib/kuro-api";

export const Route = createFileRoute("/api/public/v1/sync/devices")({
  server: {
    handlers: {
      OPTIONS: async () => preflight(),
      GET: async ({ request, env }: any) => {
        const auth = await requireUser(request, env);
        if ("error" in auth) return auth.error;

        const db = getDB(env);
        const result = await db.prepare(
          "SELECT DISTINCT device_id, MAX(created_at) as last_seen FROM sync_queue WHERE user_id = ? GROUP BY device_id ORDER BY last_seen DESC"
        ).bind(auth.userId).all();

        return json({ devices: result.results ?? [] });
      },
      POST: async ({ request, env }: any) => {
        const auth = await requireUser(request, env);
        if ("error" in auth) return auth.error;

        const body = await request.json().catch(() => null);
        if (!body?.device_id) return json({ error: "device_id_required" }, 400);

        const db = getDB(env);
        const now = new Date().toISOString();

        await db.prepare(
          "INSERT OR REPLACE INTO sync_queue (user_id, device_id, entity_type, entity_id, action, payload, created_at, processed) VALUES (?, ?, ?, ?, ?, ?, ?, 0)"
        ).bind(auth.userId, body.device_id, "device", body.device_id, "heartbeat", JSON.stringify({ user_agent: body.user_agent || null }), now).run();

        return json({ success: true });
      },
    },
  },
});
