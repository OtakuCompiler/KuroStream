import { Router, Request, Response } from 'express';
import { z } from 'zod';
import { authMiddleware, optionalAuthMiddleware, validateRequest } from '../middleware';
import { logger } from '../utils/logger';

const router = Router();

const createItemSchema = z.object({
  id: z.string().min(1),
  name: z.string().max(255),
  description: z.string().optional().nullable(),
  price: z.number().min(0),
  currency: z.string().length(3).default('USD'),
  skin_id: z.string().optional().nullable(),
  type: z.enum(['skin', 'pass', 'bundle']),
  tier: z.enum(['free', 'premium', 'exclusive']).default('free'),
  preview_image_url: z.string().url().optional().nullable(),
  preview_video_url: z.string().url().optional().nullable(),
  metadata: z.record(z.unknown()).optional(),
  is_active: z.boolean().default(true),
  sort_order: z.number().int().default(0),
});

const updateItemSchema = z.object({
  name: z.string().max(255).optional(),
  description: z.string().optional().nullable(),
  price: z.number().min(0).optional(),
  currency: z.string().length(3).optional(),
  skin_id: z.string().optional().nullable(),
  type: z.enum(['skin', 'pass', 'bundle']).optional(),
  tier: z.enum(['free', 'premium', 'exclusive']).optional(),
  preview_image_url: z.string().url().optional().nullable(),
  preview_video_url: z.string().url().optional().nullable(),
  metadata: z.record(z.unknown()).optional(),
  is_active: z.boolean().optional(),
  sort_order: z.number().int().optional(),
});

const searchSchema = z.object({
  q: z.string().optional(),
  type: z.enum(['skin', 'pass', 'bundle']).optional(),
  tier: z.enum(['free', 'premium', 'exclusive']).optional(),
  min_price: z.number().min(0).optional(),
  max_price: z.number().min(0).optional(),
  limit: z.number().int().positive().max(100).default(20),
  offset: z.number().int().min(0).default(0),
  sort: z.enum(['name', 'price', 'rating', 'created', 'popularity']).default('name'),
  order: z.enum(['asc', 'desc']).default('asc'),
});

export const marketplaceRouter = Router();

export default router;