import type {
  D1Database,
  MarketplaceItemRow,
  ProfileRow,
  PurchaseRow,
  ActiveSkinRow,
  ReportRow,
  SubmissionReviewRow,
} from "./types";

export function getDB(env: { KURO_DB: D1Database }) {
  return env.KURO_DB;
}

export async function getCatalog(db: D1Database): Promise<MarketplaceItemRow[]> {
  const { results } = await db
    .prepare(
      `SELECT id, name, author, description, long_description, category, price, rating, installs, emoji, palette, particle, is_premium, file_url, screenshots, status, submitter_id, legal_basis, reviewed_by, reviewed_at, rejection_reason, created_at, updated_at
     FROM marketplace_items WHERE status = 'approved' ORDER BY created_at DESC`,
    )
    .all<MarketplaceItemRow>();
  return results || [];
}

export async function getItemById(db: D1Database, id: string): Promise<MarketplaceItemRow | null> {
  const row = await db
    .prepare(`SELECT * FROM marketplace_items WHERE id = ?`)
    .bind(id)
    .first<MarketplaceItemRow>();
  return row || null;
}

export async function getApprovedItemById(
  db: D1Database,
  id: string,
): Promise<MarketplaceItemRow | null> {
  const row = await db
    .prepare(`SELECT * FROM marketplace_items WHERE id = ? AND status = 'approved'`)
    .bind(id)
    .first<MarketplaceItemRow>();
  return row || null;
}

export async function getProfile(db: D1Database, userId: string): Promise<ProfileRow | null> {
  const row = await db
    .prepare(`SELECT * FROM profiles WHERE id = ?`)
    .bind(userId)
    .first<ProfileRow>();
  return row || null;
}

export async function upsertProfile(db: D1Database, userId: string, displayName: string) {
  await db
    .prepare(
      `INSERT INTO profiles (id, display_name, created_at) VALUES (?, ?, datetime('now'))
     ON CONFLICT(id) DO UPDATE SET display_name = excluded.display_name`,
    )
    .bind(userId, displayName)
    .run();
}

export async function getPurchases(db: D1Database, userId: string): Promise<PurchaseRow[]> {
  const { results } = await db
    .prepare(
      `SELECT id, item_id, amount, status, created_at FROM purchases WHERE user_id = ? ORDER BY created_at DESC`,
    )
    .bind(userId)
    .all<PurchaseRow>();
  return results || [];
}

export async function insertPurchase(
  db: D1Database,
  userId: string,
  itemId: string,
  amount: number,
) {
  await db
    .prepare(
      `INSERT OR IGNORE INTO purchases (user_id, item_id, amount, status, created_at) VALUES (?, ?, ?, 'completed', datetime('now'))`,
    )
    .bind(userId, itemId, amount)
    .run();
}

export async function getActiveSkin(db: D1Database, userId: string): Promise<ActiveSkinRow | null> {
  const row = await db
    .prepare(`SELECT * FROM user_active_skin WHERE user_id = ?`)
    .bind(userId)
    .first<ActiveSkinRow>();
  return row || null;
}

export async function setActiveSkin(db: D1Database, userId: string, itemId: string) {
  await db
    .prepare(
      `INSERT INTO user_active_skin (user_id, item_id, updated_at) VALUES (?, ?, datetime('now'))
     ON CONFLICT(user_id) DO UPDATE SET item_id = excluded.item_id, updated_at = excluded.updated_at`,
    )
    .bind(userId, itemId)
    .run();
}

export async function getPurchasedItems(db: D1Database, userId: string): Promise<string[]> {
  const { results } = await db
    .prepare(`SELECT item_id FROM purchases WHERE user_id = ?`)
    .bind(userId)
    .all<{ item_id: string }>();
  return (results || []).map((r) => r.item_id);
}

export async function insertMarketplaceItem(db: D1Database, item: MarketplaceItemRow) {
  await db
    .prepare(
      `INSERT INTO marketplace_items (id, name, author, description, long_description, category, price, rating, installs, emoji, palette, particle, is_premium, file_url, screenshots, status, submitter_id, legal_basis, reviewed_by, reviewed_at, rejection_reason, created_at, updated_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
    )
    .bind(
      item.id,
      item.name,
      item.author,
      item.description,
      item.long_description,
      item.category,
      item.price,
      item.rating,
      item.installs,
      item.emoji,
      item.palette,
      item.particle,
      item.is_premium,
      item.file_url,
      item.screenshots,
      item.status,
      item.submitter_id,
      item.legal_basis,
      item.reviewed_by,
      item.reviewed_at,
      item.rejection_reason,
      item.created_at,
      item.updated_at,
    )
    .run();
}

export async function getPendingReviews(db: D1Database): Promise<SubmissionReviewRow[]> {
  const { results } = await db
    .prepare(
      `SELECT * FROM submission_reviews WHERE status = 'pending_review' ORDER BY created_at ASC`,
    )
    .all<SubmissionReviewRow>();
  return results || [];
}

export async function getReviewById(
  db: D1Database,
  itemId: string,
): Promise<SubmissionReviewRow | null> {
  const row = await db
    .prepare(`SELECT * FROM submission_reviews WHERE item_id = ?`)
    .bind(itemId)
    .first<SubmissionReviewRow>();
  return row || null;
}

export async function updateReviewStatus(
  db: D1Database,
  itemId: string,
  status: "approved" | "rejected",
  reviewedBy: string,
  rejectionReason?: string,
) {
  const now = new Date().toISOString();
  await db
    .prepare(
      `UPDATE submission_reviews SET status = ?, reviewed_by = ?, reviewed_at = ?, rejection_reason = ? WHERE item_id = ?`,
    )
    .bind(status, reviewedBy, now, rejectionReason || null, itemId)
    .run();

  if (status === "approved") {
    await db
      .prepare(
        `UPDATE marketplace_items SET status = 'approved', reviewed_by = ?, reviewed_at = ?, updated_at = ? WHERE id = ?`,
      )
      .bind(reviewedBy, now, now, itemId)
      .run();
  } else {
    await db
      .prepare(
        `UPDATE marketplace_items SET status = 'rejected', reviewed_by = ?, reviewed_at = ?, rejection_reason = ?, updated_at = ? WHERE id = ?`,
      )
      .bind(reviewedBy, now, rejectionReason || null, now, itemId)
      .run();
  }
}

export async function getReports(db: D1Database, status?: string): Promise<ReportRow[]> {
  let query = `SELECT * FROM reports`;
  if (status) {
    query += ` WHERE status = ? ORDER BY created_at DESC`;
    const { results } = await db.prepare(query).bind(status).all<ReportRow>();
    return results || [];
  }
  query += ` ORDER BY created_at DESC`;
  const { results } = await db.prepare(query).all<ReportRow>();
  return results || [];
}

export async function updateReportStatus(
  db: D1Database,
  reportId: string,
  status: "resolved" | "dismissed",
  reviewedBy: string,
) {
  const now = new Date().toISOString();
  await db
    .prepare(`UPDATE reports SET status = ?, reviewed_by = ?, reviewed_at = ? WHERE id = ?`)
    .bind(status, reviewedBy, now, reportId)
    .run();
}

export async function getUserReports(db: D1Database, userId: string): Promise<ReportRow[]> {
  const { results } = await db
    .prepare(`SELECT * FROM reports WHERE reporter_id = ? ORDER BY created_at DESC`)
    .bind(userId)
    .all<ReportRow>();
  return results || [];
}
