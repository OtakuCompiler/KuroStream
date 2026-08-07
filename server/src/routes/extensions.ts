import { Router, Request, Response } from 'express';
import { z } from 'zod';
import { authMiddleware, validateRequest, optionalAuthMiddleware } from '../middleware';
import { logger } from '../utils/logger';

const router = Router();

const createExtensionSchema = z.object({
  id: z.string().min(1),
  name: z.string().max(255),
  version: z.string().max(50),
  description: z.string().max(1000).optional(),
  author: z.string().max(255),
  author_id: z.string().min(1),
  author_verified: z.boolean().default(false),
  type: z.enum(['source', 'metadata', 'subtitle', 'utility']),
  permissions: z.array(z.object({
    name: z.string(),
    description: z.string(),
    required: z.boolean().default(true),
  })).default([]),
  manifest_url: z.string().url(),
  icon_url: z.string().url().optional().nullable(),
  screenshots: z.array(z.string().url()).default([]),
  categories: z.array(z.string()).default([]),
  supported_languages: z.array(z.string()).default([]),
  min_app_version: z.string().min(1),
  max_app_version: z.string().optional().nullable(),
});

const updateExtensionSchema = z.object({
  name: z.string().max(255).optional(),
  version: z.string().max(50).optional(),
  description: z.string().max(1000).optional(),
  icon_url: z.string().url().optional().nullable(),
  screenshots: z.array(z.string().url()).optional(),
  categories: z.array(z.string()).optional(),
  supported_languages: z.array(z.string()).optional(),
  min_app_version: z.string().optional(),
  max_app_version: z.string().optional().nullable(),
  status: z.enum(['pending', 'approved', 'rejected', 'suspended']).optional(),
  moderation_notes: z.string().optional().nullable(),
});

const installExtensionSchema = z.object({
  extension_id: z.string().min(1),
  version: z.string().max(50).optional(),
  config: z.record(z.unknown()).default({}),
});

const updateInstallationSchema = z.object({
  is_enabled: z.boolean().optional(),
  config: z.record(z.unknown()).optional(),
});

const searchExtensionsSchema = z.object({
  q: z.string().optional(),
  type: z.enum(['source', 'metadata', 'subtitle', 'utility']).optional(),
  category: z.string().optional(),
  language: z.string().optional(),
  limit: z.number().int().positive().max(100).default(20),
  offset: z.number().int().min(0).default(0),
  sort: z.enum(['name', 'rating', 'downloads', 'updated']).default('name'),
  order: z.enum(['asc', 'desc']).default('asc'),
});

export const extensionsRouter = Router();

export default router;