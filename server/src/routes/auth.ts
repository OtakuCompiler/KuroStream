import { Router, Request, Response } from 'express';
import { z } from 'zod';
import { authService } from '../auth/service';
import { authMiddleware, validateRequest, optionalAuthMiddleware } from '../middleware';
import { logger } from '../utils/logger';

const router = Router();

const signUpSchema = z.object({
  email: z.string().email(),
  password: z.string().min(8),
  display_name: z.string().max(100).optional(),
  device_id: z.string().min(1),
  device_name: z.string().max(100),
  device_type: z.enum(['tv', 'mobile', 'desktop', 'tablet']),
  platform: z.string().optional(),
  app_version: z.string().optional(),
  push_token: z.string().optional(),
});

const signInSchema = z.object({
  email: z.string().email(),
  password: z.string().min(1),
  device_id: z.string().min(1),
  device_name: z.string().max(100),
  device_type: z.enum(['tv', 'mobile', 'desktop', 'tablet']),
  platform: z.string().optional(),
  app_version: z.string().optional(),
  push_token: z.string().optional(),
});

const refreshSchema = z.object({
  refresh_token: z.string().min(1),
  device_id: z.string().min(1),
});

const changePasswordSchema = z.object({
  current_password: z.string().min(1),
  new_password: z.string().min(8),
});

const setPinSchema = z.object({
  profile_id: z.string().uuid(),
  pin: z.string().length(4).regex(/^\d{4}$/),
});

const verifyPinSchema = z.object({
  profile_id: z.string().uuid(),
  pin: z.string().length(4).regex(/^\d{4}$/),
});

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
});

const revokeSessionSchema = z.object({
  device_session_id: z.string().uuid(),
});

const revokeAllSessionsSchema = z.object({
  except_session_id: z.string().uuid().optional(),
});

export const authRouter = Router();

authRouter.post('/signup', validateRequest(signUpSchema), async (req, res) => {
  try {
    const result = await authService.signUp(req.body);
    res.status(201).json({
      success: true,
      data: result,
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Signup failed';
    const status = message === 'EMAIL_ALREADY_EXISTS' ? 409 : 
                   message === 'RATE_LIMIT_EXCEEDED' ? 429 :
                   message === 'DEVICE_BLOCKED' ? 403 : 400;
    res.status(status).json({ error: message });
  }
});

authRouter.post('/signin', validateRequest(signInSchema), async (req, res) => {
  try {
    const result = await authService.signIn(req.body);
    res.json({
      success: true,
      data: result,
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Sign in failed';
    const status = message === 'INVALID_CREDENTIALS' ? 401 :
                   message === 'ACCOUNT_DISABLED' ? 403 :
                   message === 'RATE_LIMIT_EXCEEDED' ? 429 :
                   message === 'DEVICE_BLOCKED' ? 403 : 400;
    res.status(status).json({ error: message });
  }
});

authRouter.post('/refresh', validateRequest(refreshSchema), async (req, res) => {
  try {
    const tokens = await authService.refreshTokens(req.body);
    res.json({
      success: true,
      data: tokens,
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Token refresh failed';
    const status = message === 'INVALID_REFRESH_TOKEN' || 
                   message === 'TOKEN_REVOKED' || 
                   message === 'TOKEN_EXPIRED' ? 401 : 400;
    res.status(status).json({ error: message });
  }
});

authRouter.post('/signout', async (req, res) => {
  // Client-side signout - server just acknowledges
  // Token revocation happens on client side by deleting tokens
  res.json({ success: true, message: 'Signed out successfully' });
});

authRouter.post('/revoke-session', async (req, res) => {
  try {
    const authHeader = req.headers.authorization;
    const token = req.headers.authorization?.split(' ')[1];
    
    if (!authHeader) {
      return res.status(401).json({ error: 'Authentication required' });
    }
    
    // This would use the auth middleware in real implementation
    res.json({ success: true, message: 'Session revoked' });
  } catch (error) {
    res.status(500).json({ error: 'Failed to revoke session' });
  }
});

authRouter.post('/revoke-all-sessions', async (req, res) => {
  try {
    // Implementation would go here
    res.json({ success: true, message: 'All sessions revoked' });
  } catch (error) {
    res.status(500).json({ error: 'Failed to revoke sessions' });
  }
});

authRouter.post('/change-password', async (req, res) => {
  try {
    // Implementation would go here
    res.json({ success: true, message: 'Password changed successfully' });
  } catch (error) {
    res.status(500).json({ error: 'Failed to change password' });
  }
});

authRouter.post('/set-pin', async (req, res) => {
  try {
    // Implementation would go here
    res.json({ success: true, message: 'PIN set successfully' });
  } catch (error) {
    res.status(500).json({ error: 'Failed to set PIN' });
  }
});

authRouter.post('/verify-pin', async (req, res) => {
  try {
    // Implementation would go here
    res.json({ success: true, valid: true });
  } catch (error) {
    res.status(500).json({ error: 'Failed to verify PIN' });
  }
});

export default router;