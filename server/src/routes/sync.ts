import { Router, Request, Response } from 'express';
import { z } from 'zod';
import { authMiddleware, validateRequest, optionalAuthMiddleware } from '../middleware';
import { logger } from '../utils/logger';

const router = Router();

const claimFreeSchema = z.object({
  item_id: z.string().min(1),
});

const setActiveSkinSchema = z.object({
  skin_id: z.string().min(1),
});

const updateEntitlementsSchema = z.object({
  owned_item_ids: z.array(z.string()).optional(),
  has_skins_pass: z.boolean().optional(),
  active_skin_id: z.string().optional().nullable(),
});

const bulkSyncSchema = z.object({
  entitlements: z.object({
    owned_item_ids: z.array(z.string()).optional(),
    has_skins_pass: z.boolean().optional(),
    active_skin_id: z.string().optional().nullable(),
  }).optional(),
  catalog: z.array(z.object({
    id: z.string(),
    name: z.string(),
    description: z.string().optional(),
    price: z.number(),
    currency: z.string().optional(),
    skin_id: z.string().optional().nullable(),
    type: z.enum(['skin', 'pass', 'bundle']),
    tier: z.enum(['free', 'premium', 'exclusive']).optional(),
    preview_image_url: z.string().optional().nullable(),
    preview_video_url: z.string().optional().nullable(),
    metadata: z.record(z.unknown()).optional(),
  }).optional(),
  purchases: z.array(z.object({
    item_id: z.string(),
    amount: z.number(),
    currency: z.string().optional(),
    status: z.enum(['pending', 'completed', 'failed', 'refunded']),
    provider: z.string().optional(),
    provider_transaction_id: z.string().optional(),
    metadata: z.record(z.unknown()).optional(),
    created_at: z.string().datetime().optional(),
  }).optional(),
});

export const syncRouter = Router();

export default router;