// Security hardening utilities and shared validation schemas
import { z } from "zod";
import type { KVNamespace } from "@/integrations/cloudflare/types";

export const schemas = {
  catalogQuery: z.object({
    category: z.enum(["skin", "pack", "addon", "pass", "all"]).optional(),
    search: z.string().max(100).optional(),
    limit: z.coerce.number().min(1).max(100).optional(),
    offset: z.coerce.number().min(0).optional(),
  }),
  purchaseBody: z.object({ item_id: z.string().min(1).max(120) }),
  activeSkinBody: z.object({ item_id: z.string().min(1).max(120) }),
  submitBody: z.object({
    id: z.string().min(1).max(120),
    name: z.string().min(1).max(120),
    author: z.string().min(1).max(120),
    description: z.string().min(10).max(500),
    long_description: z.string().max(5000).optional(),
    category: z.enum(["skin", "pack", "addon", "pass"]),
    price: z.number().min(0).max(10000),
    manifest: z.record(z.unknown()).optional(),
    screenshots: z.array(z.string().url()).max(10).optional(),
    legal_basis: z.string().min(10).max(2000),
    aup_accepted: z.boolean(),
  }),
  reportBody: z.object({
    item_id: z.string().min(1).max(120),
    reason: z.enum(["piracy", "copyright", "malware", "misleading", "spam", "other"]),
    details: z.string().max(2000).optional(),
  }),
  reviewActionBody: z.object({
    item_id: z.string().min(1).max(120),
    action: z.enum(["approve", "reject"]),
    rejection_reason: z.string().max(1000).optional(),
  }),
  reportActionBody: z.object({
    report_id: z.string().min(1).max(120),
    action: z.enum(["resolve", "dismiss"]),
  }),
  profileBody: z.object({
    display_name: z.string().min(1).max(50).optional(),
    avatar_url: z.string().url().optional(),
  }),
  entitlementsQuery: z.object({ user_id: z.string().min(1).max(120) }),
  checkoutBody: z.object({ item_id: z.string().min(1).max(120) }),
};

export const RATE_LIMITS = {
  catalog: { limit: 120, window: 60 },
  itemDetail: { limit: 60, window: 60 },
  purchases: { limit: 60, window: 60 },
  purchasesWrite: { limit: 10, window: 60 },
  activeSkin: { limit: 30, window: 60 },
  me: { limit: 60, window: 60 },
  submit: { limit: 5, window: 3600 },
  report: { limit: 10, window: 3600 },
  adminReviews: { limit: 60, window: 60 },
  adminReviewAction: { limit: 30, window: 60 },
  adminReports: { limit: 60, window: 60 },
  adminReportAction: { limit: 30, window: 60 },
  checkout: { limit: 10, window: 60 },
} as const;

export function generateCsrfToken(): string {
  return crypto.randomUUID();
}

export async function verifyCsrfToken(request: Request, env: unknown): Promise<boolean> {
  const e = env as Record<string, unknown>;
  const kv = e.KURO_KV as KVNamespace;
  const cookieHeader = request.headers.get("cookie") ?? "";
  const csrfCookie = cookieHeader.split("; ").find((c) => c.startsWith("csrf_token="));
  const csrfToken = csrfCookie?.split("=")[1];
  const headerToken = request.headers.get("x-csrf-token");
  if (!csrfToken || !headerToken || csrfToken !== headerToken) return false;
  const stored = await kv.get("csrf:" + csrfToken);
  return !!stored;
}

export async function createCsrfCookie(env: unknown): Promise<string> {
  const token = generateCsrfToken();
  const e = env as Record<string, unknown>;
  const kv = e.KURO_KV as KVNamespace;
  await kv.put("csrf:" + token, "1", { expirationTtl: 3600 });
  return token;
}

export function sanitizeHtml(input: string): string {
  return input
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#x27;");
}

export function sql<T extends string>(
  strings: TemplateStringsArray,
  ...values: unknown[]
): { sql: string; params: unknown[] } {
  let sql = strings[0];
  const params: unknown[] = [];
  for (let i = 0; i < values.length; i++) {
    sql += "?" + strings[i + 1];
    params.push(values[i]);
  }
  return { sql, params };
}