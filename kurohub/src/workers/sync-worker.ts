// KuroStream Cross-Device Sync Worker
// Handles real-time WebSocket connections for cross-device sync
import { verifyFirebaseIdToken, extractBearerToken } from "../lib/firebase-auth";
import { getDB, getKV } from "../lib/kuro-api";

export { SyncRoom } from "./sync-room";

export default {
  async fetch(request: Request, env: unknown, ctx: ExecutionContext): Promise<Response> {
    const url = new URL(request.url);

    if (url.pathname === "/health") {
      return new Response(JSON.stringify({ status: "ok", service: "kurostream-sync" }), {
        headers: { "Content-Type": "application/json" },
      });
    }

    if (url.pathname === "/ws" || url.pathname === "/sync") {
      const token = extractBearerToken(request);
      if (!token) {
        return new Response(JSON.stringify({ error: "missing_token" }), {
          status: 401,
          headers: { "Content-Type": "application/json" },
        });
      }

      const projectId = (env as any).VITE_FIREBASE_PROJECT_ID || "kurostream13";
      const payload = await verifyFirebaseIdToken(token, projectId);
      if (!payload) {
        return new Response(JSON.stringify({ error: "invalid_token" }), {
          status: 401,
          headers: { "Content-Type": "application/json" },
        });
      }

      const userId = payload.user_id;
      const roomId = url.searchParams.get("room") || `user:${userId}`;

      const durableObjectId = (env as any).SYNC_ROOM.idFromName(roomId);
      const durableObject = (env as any).SYNC_ROOM.get(durableObjectId);

      return durableObject.fetch(request, env, ctx);
    }

    return new Response("KuroStream Sync Worker", { status: 200 });
  },
};
