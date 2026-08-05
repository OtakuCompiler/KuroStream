// Shared environment interface for Cloudflare Workers bindings
import type { D1Database, KVNamespace } from "@/integrations/cloudflare/types";

export interface AppEnv {
  // Cloudflare bindings
  KURO_DB: D1Database;
  KURO_KV: KVNamespace;

  // Firebase config (server-side)
  VITE_FIREBASE_PROJECT_ID: string;

  // Stripe
  STRIPE_SECRET_KEY: string;
  STRIPE_WEBHOOK_SECRET: string;

  // Admin
  ADMIN_KEY: string;
  ADMIN_EMAILS?: string;

  // Future: Custom claims verification key
  FIREBASE_ADMIN_PRIVATE_KEY?: string;
  FIREBASE_ADMIN_CLIENT_EMAIL?: string;
  FIREBASE_ADMIN_PROJECT_ID?: string;
}

export function getEnv<T extends keyof AppEnv>(env: unknown, key: T): AppEnv[T] {
  const e = env as Record<string, unknown>;
  const value = e[key];
  if (value === undefined) {
    throw new Error(`Required environment variable ${key} is not set`);
  }
  return value as AppEnv[T];
}

export function getOptionalEnv<T extends keyof AppEnv>(env: unknown, key: T): AppEnv[T] | undefined {
  const e = env as Record<string, unknown>;
  return e[key] as AppEnv[T] | undefined;
}

// Type guard to check if env has required bindings
export function hasRequiredBindings(env: unknown): env is AppEnv {
  const e = env as Record<string, unknown>;
  return (
    typeof e.KURO_DB !== "undefined" &&
    typeof e.KURO_KV !== "undefined" &&
    typeof e.VITE_FIREBASE_PROJECT_ID === "string"
  );
}