export interface D1Database {
  prepare(query: string): D1PreparedStatement;
}

export interface D1PreparedStatement<T = any> {
  bind(...values: unknown[]): D1BoundStatement<T>;
  first<R = any>(): Promise<R | null>;
  all<R = any>(): Promise<{ results: R[] }>;
  run(): Promise<any>;
}

export interface D1BoundStatement<T = any> {
  first<R = any>(): Promise<R | null>;
  all<R = any>(): Promise<{ results: R[] }>;
  run(): Promise<any>;
}

export interface KVNamespace {
  get(key: string, options?: { type: "json" }): Promise<any>;
  put(key: string, value: string, options?: { expirationTtl?: number }): Promise<void>;
}

export interface Env {
  KURO_DB: D1Database;
  KURO_KV: KVNamespace;
}

export interface MarketplaceItemRow {
  id: string;
  name: string;
  author: string;
  description: string;
  long_description: string;
  category: string;
  price: number;
  rating: number;
  installs: number;
  emoji: string | null;
  palette: string | null; // JSON string
  particle: string | null;
  is_premium: number; // 0 or 1
  file_url: string | null;
  screenshots: string | null; // JSON string
  status: string;
  submitter_id: string | null;
  legal_basis: string | null;
  reviewed_by: string | null;
  reviewed_at: string | null;
  rejection_reason: string | null;
  created_at: string;
  updated_at: string;
}

export interface ProfileRow {
  id: string;
  display_name: string | null;
  avatar_url: string | null;
  created_at: string;
}

export interface PurchaseRow {
  id: string;
  user_id: string;
  item_id: string;
  amount: number;
  status: string;
  created_at: string;
}

export interface ActiveSkinRow {
  user_id: string;
  item_id: string;
  updated_at: string;
}

export interface ReportRow {
  id: string;
  item_id: string;
  reporter_id: string;
  reason: string;
  details: string | null;
  created_at: string;
  status: string;
  reviewed_by: string | null;
  reviewed_at: string | null;
}

export interface SubmissionReviewRow {
  id: string;
  item_id: string;
  submitter_id: string;
  manifest: string;
  description: string;
  screenshots: string | null;
  legal_basis: string;
  aup_accepted: number;
  aup_accepted_at: string | null;
  status: string;
  auto_reject_reasons: string | null;
  reviewed_by: string | null;
  reviewed_at: string | null;
  rejection_reason: string | null;
  created_at: string;
}
