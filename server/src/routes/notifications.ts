import { Router, Request, Response } from 'express';
import { z } from 'zod';
import { authMiddleware, validateRequest } from '../middleware';
import { logger } from '../utils/logger';

const router = Router();

const sendNotificationSchema = z.object({
  token: z.string().min(1),
  title: z.string().max(100),
  body: z.string().max(500),
  data: z.record(z.string()).optional(),
  channel_id: z.string().optional(),
  priority: z.enum(['high', 'normal', 'low']).default('high'),
});

const sendTopicNotificationSchema = z.object({
  topic: z.string().min(1),
  title: z.string().max(100),
  body: z.string().max(500),
  data: z.record(z.string()).optional(),
  channel_id: z.string().optional(),
  priority: z.enum(['high', 'normal', 'low']).default('high'),
  notification_icon: z.string().optional(),
  notification_color: z.string().optional(),
});

const subscribeSchema = z.object({
  token: z.string().min(1),
  topic: z.string().min(1),
});

const unsubscribeSchema = z.object({
  token: z.string().min(1),
  topic: z.string().min(1),
});

const newEpisodeSchema = z.object({
  show_title: z.string().max(255),
  episode_title: z.string().max(255),
  episode_number: z.string().max(50),
  media_id: z.string().min(1),
  topic: z.string().min(1),
});

export const notificationsRouter = Router();

export default router;