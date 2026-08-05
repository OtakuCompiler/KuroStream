// Shared middleware for API routes
import { verifyFirebaseIdToken, extractBearerToken } from "./firebase-auth";
import { json, checkRateLimit, CORS_HEADERS } from "./kuro-api";
import { getEnv, hasRequiredBindings, type AppEnv } from "./env";
import type { KVNamespace, D1Database } from "@/integrations/cloudflare/types";
import { z } from "zod";
import { timingSafeCompare } from "./kuro-api";

// Rate limit tiers (imported from security.ts to avoid duplication)
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

// Request validation error
export class ValidationError extends Error {
  constructor(
    message: string,
    public readonly issues: z.ZodIssue[],
  ) {
    super(message);
    this.name = "ValidationError";
  }
}

// Auth error
export class AuthError extends Error {
  constructor(
    message: string,
    public readonly status: number = 401,
  ) {
    super(message);
    this.name = "AuthError";
  }
}

// Admin error
export class AdminError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "AdminError";
  }
}

// Bindings error
export class BindingsError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "BindingsError";
  }
}

/**
 * Validates request body against a Zod schema.
 * Returns parsed data on success, throws ValidationError on failure.
 */
export async function validateBody<T>(
  request: Request,
  schema: z.ZodSchema<T>,
): Promise<T> {
  const body = await request.json().catch(() => null);
  const result = schema.safeParse(body);
  if (!result.success) {
    throw new ValidationError("Invalid request body", result.error.issues);
  }
  return result.data;
}

/**
 * Validates query params against a Zod schema.
 */
export function validateQuery<T>(
  request: Request,
  schema: z.ZodSchema<T>,
): T {
  const url = new URL(request.url);
  const params: Record<string, string> = {};
  url.searchParams.forEach((value, key) => {
    params[key] = value;
  });
  const result = schema.safeParse(params);
  if (!result.success) {
    throw new ValidationError("Invalid query parameters", result.error.issues);
  }
  return result.data;
}

/**
 * Creates a standard JSON error response for validation errors.
 */
export function validationErrorResponse(error: ValidationError): Response {
  return json(
    {
      error: "invalid_request",
      details: error.issues.map((i) => ({
        path: i.path.join("."),
        message: i.message,
      })),
    },
    400,
  );
}

/**
 * Rate limit check with standard error response.
 */
export async function rateLimitCheck(
  kv: KVNamespace,
  key: string,
  limit: number,
  windowSeconds: number,
): Promise<{ allowed: boolean; remaining: number; reset: number }> {
  return checkRateLimit(kv, key, limit, windowSeconds);
}

export function rateLimitResponse(
  rate: { allowed: boolean; remaining: number; reset: number },
): Response | null {
  if (rate.allowed) return null;
  return json({ error: "rate_limited", reset: rate.reset }, 429);
}

/**
 * Verifies Firebase ID token and extracts user ID.
 * Returns userId on success, throws AuthError on failure.
 */
export async function requireUser(request: Request, env: unknown): Promise<string> {
  const token = extractBearerToken(request);
  if (!token) throw new AuthError("Missing bearer token", 401);

  if (!hasRequiredBindings(env)) {
    throw new BindingsError("Firebase project ID not configured");
  }

  const projectId = getEnv(env, "VITE_FIREBASE_PROJECT_ID");
  const payload = await verifyFirebaseIdToken(token, projectId);
  if (!payload) {
    throw new AuthError("Invalid token", 401);
  }

  return payload.user_id;
}

/**
 * Verifies admin access using timing-safe comparison of admin key.
 * Returns userId on success, throws AdminError on failure.
 */
export async function requireAdmin(
  request: Request,
  env: unknown,
  userId: string,
): Promise<void> {
  const adminKey = request.headers.get("x-admin-key");
  const expectedKey = getEnv(env, "ADMIN_KEY");

  if (!adminKey) throw new AdminError("Admin key required");

  const isValid = await timingSafeCompare(adminKey, expectedKey);
  if (!isValid) throw new AdminError("Invalid admin key");
}

/**
 * Middleware wrapper for API handlers.
 * Handles: validation, auth, admin check, rate limiting, bindings.
 */
export interface HandlerContext {
  env: AppEnv;
  request: Request;
  userId: string;
  db: D1Database;
  kv: KVNamespace;
  rateLimit: { allowed: boolean; remaining: number; reset: number };
}

export type MiddlewareOptions = {
  requireAuth?: boolean;
  requireAdmin?: boolean;
  rateLimit?: { limit: number; window: number; keyPrefix: string };
  validateBody?: z.ZodSchema;
  validateQuery?: z.ZodSchema;
};

export async function withMiddleware(
  request: Request,
  env: unknown,
  options: MiddlewareOptions = {},
  handler: (ctx: HandlerContext) => Promise<Response>,
): Promise<Response> {
  const startTime = Date.now();
  const requestId = crypto.randomUUID();

  try {
    // Check required bindings
    if (!hasRequiredBindings(env)) {
      throw new BindingsError("Required Cloudflare bindings not available");
    }

    const appEnv = env as AppEnv;
    const kv = appEnv.KURO_KV;
    const db = appEnv.KURO_DB;

    // Rate limiting
    let rateLimitInfo = { allowed: true, remaining: 999, reset: 0 };
    if (options.rateLimit) {
      const clientIp = request.headers.get("cf-connecting-ip") ?? "unknown";
      const key = `${options.rateLimit.keyPrefix}:${options.requireAuth ? "user" : "ip"}:${clientIp}`;
      rateLimitInfo = await checkRateLimit(kv, key, options.rateLimit.limit, options.rateLimit.window);
      const rateResponse = rateLimitResponse(rateLimitInfo);
      if (rateResponse) return rateResponse;
    }

    // Auth
    let userId = "";
    if (options.requireAuth) {
      userId = await requireUser(request, env);
    }

    // Admin check
    if (options.requireAdmin) {
      if (!userId) throw new AuthError("Authentication required for admin check");
      await requireAdmin(request, env, userId);
    }

    // Body validation
    if (options.validateBody) {
      try {
        await validateBody(request, options.validateBody);
      } catch (error) {
        if (error instanceof ValidationError) return validationErrorResponse(error);
        throw error;
      }
    }

    // Query validation
    if (options.validateQuery) {
      try {
        validateQuery(request, options.validateQuery);
      } catch (error) {
        if (error instanceof ValidationError) return validationErrorResponse(error);
        throw error;
      }
    }

    const ctx: HandlerContext = {
      env: appEnv,
      request,
      userId,
      db,
      kv,
      rateLimit: rateLimitInfo,
    };

    const response = await handler(ctx);

    // Add timing header
    const duration = Date.now() - startTime;
    response.headers.set("X-Request-Duration", `${duration}ms`);
    response.headers.set("X-Request-ID", requestId);

    return response;
  } catch (error) {
    const duration = Date.now() - startTime;
    console.error(`[${requestId}] Handler error:`, error);

    let response: Response;
    if (error instanceof ValidationError) {
      response = validationErrorResponse(error);
    } else if (error instanceof AuthError) {
      response = json({ error: error.message }, error.status);
    } else if (error instanceof AdminError) {
      response = json({ error: "admin_required" }, 403);
    } else if (error instanceof BindingsError) {
      response = json({ error: "storage_unavailable" }, 503);
    } else {
      response = json({ error: "internal_error" }, 500);
    }

    response.headers.set("X-Request-Duration", `${duration}ms`);
    response.headers.set("X-Request-ID", requestId);
    return response;
  }
}

/**
 * Helper to create preflight response.
 */
export function preflightResponse(): Response {
  return new Response(null, { status: 204, headers: CORS_HEADERS });
}