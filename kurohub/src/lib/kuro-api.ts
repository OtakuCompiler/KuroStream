// Shared helpers for the public KuroStream Sync API (/api/public/v1/*).
// Server-side only — uses Cloudflare D1 bindings.
import { verifyFirebaseIdToken, extractBearerToken } from "./firebase-auth";
import type { D1Database, KVNamespace } from "@/integrations/cloudflare/types";

export const CORS_HEADERS: Record<string, string> = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET,POST,OPTIONS",
  "Access-Control-Allow-Headers": "authorization, content-type, apikey",
  "Access-Control-Max-Age": "86400",
};

export function json(body: unknown, status = 200, extraHeaders: Record<string, string> = {}) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      ...CORS_HEADERS,
      ...extraHeaders,
    },
  });
}

export function preflight() {
  return new Response(null, { status: 204, headers: CORS_HEADERS });
}

export function getDB(env: unknown): D1Database {
  const e = env as Record<string, unknown>;
  if (!e.KURO_DB) {
    throw new Response(JSON.stringify({ error: "storage_unavailable" }), {
      status: 503,
      headers: { "content-type": "application/json; charset=utf-8", ...CORS_HEADERS },
    });
  }
  return e.KURO_DB as D1Database;
}

export function getKV(env: unknown): KVNamespace {
  const e = env as Record<string, unknown>;
  if (!e.KURO_KV) {
    throw new Response(JSON.stringify({ error: "storage_unavailable" }), {
      status: 503,
      headers: { "content-type": "application/json; charset=utf-8", ...CORS_HEADERS },
    });
  }
  return e.KURO_KV as KVNamespace;
}

export function getEnvVar(env: unknown, key: string): string | undefined {
  const e = env as Record<string, unknown>;
  return e[key] as string | undefined;
}

/**
 * Generate a cryptographically secure random ID.
 */
export function secureId(): string {
  return crypto.randomUUID();
}

/**
 * Timing-safe comparison for secrets/tokens.
 */
export async function timingSafeCompare(a: string, b: string): Promise<boolean> {
  const encoder = new TextEncoder();
  const [hashA, hashB] = await Promise.all([
    crypto.subtle.digest("SHA-256", encoder.encode(a)),
    crypto.subtle.digest("SHA-256", encoder.encode(b)),
  ]);
  const aBytes = new Uint8Array(hashA);
  const bBytes = new Uint8Array(hashB);
  if (aBytes.length !== bBytes.length) return false;
  let result = 0;
  for (let i = 0; i < aBytes.length; i++) {
    result |= aBytes[i]! ^ bBytes[i]!;
  }
  return result === 0;
}

/**
 * Verify Firebase ID token and extract user ID.
 */
export async function requireUser(
  request: Request,
  env: unknown,
): Promise<{ userId: string } | { error: Response }> {
  const token = extractBearerToken(request);
  if (!token) return { error: json({ error: "missing_bearer_token" }, 401) };

  const projectId = getEnvVar(env, "VITE_FIREBASE_PROJECT_ID");
  if (!projectId) {
    console.error("FIREBASE_PROJECT_ID not configured");
    return { error: json({ error: "auth_not_configured" }, 503) };
  }

  const payload = await verifyFirebaseIdToken(token, projectId);
  if (!payload) {
    return { error: json({ error: "invalid_token" }, 401) };
  }

  return { userId: payload.user_id };
}

/**
 * Rate limiting helper using KV.
 */
export async function checkRateLimit(
  kv: KVNamespace,
  key: string,
  limit: number,
  windowSeconds: number,
): Promise<{ allowed: boolean; remaining: number; reset: number }> {
  const now = Math.floor(Date.now() / 1000);
  const windowStart = Math.floor(now / windowSeconds) * windowSeconds;
  const kvKey = `ratelimit:${key}:${windowStart}`;

  const current = await kv.get(kvKey);
  const count = current ? parseInt(current, 10) : 0;

  if (count >= limit) {
    return { allowed: false, remaining: 0, reset: windowStart + windowSeconds };
  }

  await kv.put(kvKey, String(count + 1), { expirationTtl: windowSeconds });
  return { allowed: true, remaining: limit - count - 1, reset: windowStart + windowSeconds };
}