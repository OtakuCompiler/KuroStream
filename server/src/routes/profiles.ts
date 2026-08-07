import { Router, Request, Response } from 'express';
import { z } from 'zod';
import { authMiddleware, validateRequest } from '../middleware';
import { logger } from '../utils/logger';

const router = Router();

const updateProfileSchema = z.object({
  display_name: z.string().max(100).optional(),
  avatar_url: z.string().url().optional().nullable(),
  language: z.string().length(2).optional(),
  subtitle_language: z.string().length(2).optional(),
  parental_controls: z.object({
    max_rating: z.string().optional(),
    block_unrated: z.boolean().optional(),
    require_pin_for_purchases: z.boolean().optional(),
    watch_time_limit_minutes: z.number().int().positive().optional().nullable(),
    blocked_categories: z.array(z.string()).optional(),
  }).optional(),
  active_skin_id: z.string().optional().nullable(),
  is_kids_mode: z.boolean().optional(),
});

const setPinSchema = z.object({
  pin: z.string().length(4).regex(/^\d{4}$/),
});

const verifyPinSchema = z.object({
  pin: z.string().length(4).regex(/^\d{4}$/),
});

export const profilesRouter = Router();

export default router;