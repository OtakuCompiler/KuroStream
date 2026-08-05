import { createFileRoute } from "@tanstack/react-router";
import { json, preflight, checkRateLimit } from "@/lib/kuro-api";
import { getCatalog } from "@/integrations/cloudflare/db";
import { RATE_LIMITS } from "@/lib/middleware";
import { hasRequiredBindings } from "@/lib/env";

const CACHE_TTL = 60;
const CACHE_KEY = "catalog:public:v1";

export const Route = createFileRoute("/api/public/v1/catalog")({
  server: {
    handlers: {
      OPTIONS: async () => preflight(),
      GET: async ({ request, env }: { request: Request; env: unknown }) => {
        if (!hasRequiredBindings(env)) {
          return json({ error: "storage_unavailable" }, 503);
        }

        const appEnv = env as Record<string, unknown>;
        const kv = appEnv.KURO_KV;
        const db = appEnv.KURO_DB;

        const clientIp = request.headers.get("cf-connecting-ip") ?? "unknown";
        const rate = await checkRateLimit(kv, `catalog:${clientIp}`, RATE_LIMITS.catalog.limit, RATE_LIMITS.catalog.window);
        if (!rate.allowed) {
          return json({ error: "rate_limited", reset: rate.reset }, 429);
        }

        const cached = await kv.get(CACHE_KEY, { type: "json" });
        if (cached) {
          return json(
            {
              ...cached,
              rate_limit: { remaining: rate.remaining, reset: rate.reset },
              cached: true,
            },
            200,
            { "Cache-Control": `public, max-age=${CACHE_TTL}, stale-while-revalidate=30` },
          );
        }

        const items = await getCatalog(db);
        const response = { items, rate_limit: { remaining: rate.remaining, reset: rate.reset } };

        await kv.put(CACHE_KEY, JSON.stringify(response), { expirationTtl: CACHE_TTL });

        return json(response, 200, {
          "Cache-Control": `public, max-age=${CACHE_TTL}, stale-while-revalidate=30`,
        });
      },
    },
  },
});