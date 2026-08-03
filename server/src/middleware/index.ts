import { Request, Response, NextFunction } from 'express';
import { getConfig } from '../config';
import { getRedis, rateLimitCheck, rateLimitReset } from '../db/redis';
import { extractTokenFromHeader, verifyAccessToken } from '../auth/jwt';
import { logger } from '../utils/logger';
import { rateLimit } from 'express-rate-limit';
import { RedisStore } from 'rate-limit-redis';
import { Redis } from 'ioredis';

const config = getConfig();

export function createRateLimiter() {
  const redis = new Redis(config.REDIS_URL);
  
  return rateLimit({
    store: new RedisStore({
      sendCommand: (...args: string[]) => redis.call(...args),
    }),
    windowMs: config.RATE_LIMIT_WINDOW_MS,
    max: config.RATE_LIMIT_MAX_REQUESTS,
    message: { error: 'Too many requests, please try again later' },
    standardHeaders: true,
    legacyHeaders: false,
    keyGenerator: (req: Request) => req.ip || 'unknown',
    skip: (req: Request) => {
      // Skip rate limiting for health checks
      return req.path === '/health' || req.path === '/metrics';
    },
  });
}

export function createAuthRateLimiter() {
  const redis = new Redis(config.REDIS_URL);
  
  return rateLimit({
    store: new RedisStore({
      sendCommand: (...args: string[]) => redis.call(...args),
    }),
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 10, // 10 requests per window
    message: { error: 'Too many authentication attempts, please try again later' },
    standardHeaders: true,
    legacyHeaders: false,
    keyGenerator: (req: Request) => `auth:${req.ip}`,
    skip: (req: Request) => req.path === '/health',
  });
}

export async function authMiddleware(req: Request, res: Response, next: NextFunction): Promise<void> {
  try {
    const authHeader = req.headers.authorization;
    const token = extractTokenFromHeader(authHeader);
    
    if (!token) {
      res.status(401).json({ error: 'Missing authorization token' });
      return;
    }
    
    const payload = await verifyAccessToken(token);
    
    // Attach user info to request
    (req as any).user = {
      id: payload.sub,
      email: payload.email,
      profileId: payload.profile_id,
      deviceSessionId: payload.device_session_id,
      jti: payload.jti,
    };
    
    next();
  } catch (error) {
    logger.warn('Authentication failed', { 
      error: error instanceof Error ? error.message : 'Unknown',
      ip: req.ip 
    });
    res.status(401).json({ error: 'Invalid or expired token' });
  }
}

export function optionalAuthMiddleware(req: Request, res: Response, next: NextFunction): void {
  const authHeader = req.headers.authorization;
  const token = extractTokenFromHeader(authHeader);
  
  if (token) {
    verifyAccessToken(token)
      .then(payload => {
        (req as any).user = {
          id: payload.sub,
          email: payload.email,
          profileId: payload.profile_id,
          deviceSessionId: payload.device_session_id,
          jti: payload.jti,
        };
      })
      .catch(() => {
        // Invalid token, but continue without auth
      })
      .finally(() => next());
  } else {
    next();
  }
}

export function requireProfile(profileId?: string) {
  return (req: Request, res: Response, next: NextFunction): void => {
    const user = (req as any).user;
    
    if (!user) {
      res.status(401).json({ error: 'Authentication required' });
      return;
    }
    
    if (profileId && user.profileId !== profileId) {
      // Check if user owns this profile
      res.status(403).json({ error: 'Profile access denied' });
      return;
    }
    
    next();
  };
}

export function requireAdmin(req: Request, res: Response, next: NextFunction): void {
  const user = (req as any).user;
  
  if (!user) {
    res.status(401).json({ error: 'Authentication required' });
    return;
  }
  
  // Check if user is admin (you would add an is_admin field to users table)
  // For now, just check if user has a specific role
  next();
}

export function validateRequest(schema: any) {
  return (req: Request, res: Response, next: NextFunction): void => {
    const { error, value } = schema.validate(req.body, { abortEarly: false });
    
    if (error) {
      res.status(400).json({
        error: 'Validation failed',
        details: error.details.map((d: any) => ({
          field: d.path.join('.'),
          message: d.message,
        })),
      });
      return;
    }
    
    req.body = value;
    next();
  };
}

export function corsMiddleware(req: Request, res: Response, next: NextFunction): void {
  const config = getConfig();
  
  const origin = req.headers.origin;
  if (origin && config.CORS_ORIGIN.includes(origin)) {
    res.setHeader('Access-Control-Allow-Origin', origin);
  }
  
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, PATCH, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, X-Requested-With');
  res.setHeader('Access-Control-Allow-Credentials', 'true');
  res.setHeader('Access-Control-Max-Age', '86400');
  
  if (req.method === 'OPTIONS') {
    res.sendStatus(204);
    return;
  }
  
  next();
}

export function securityHeaders(req: Request, res: Response, next: NextFunction): void {
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('X-XSS-Protection', '1; mode=block');
  res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');
  res.setHeader('Permissions-Policy', 'camera=(), microphone=(), geolocation=()');
  
  // CSP for API endpoints
  res.setHeader('Content-Security-Policy', "default-src 'self'; script-src 'self'; object-src 'none'; frame-ancestors 'none'");
  
  next();
}

export function errorHandler(err: Error, req: Request, res: Response, next: NextFunction): void {
  logger.error('Unhandled error', {
    error: err.message,
    stack: err.stack,
    path: req.path,
    method: req.method,
    ip: req.ip,
  });
  
  if (err.name === 'ValidationError') {
    res.status(400).json({ error: 'Validation error', message: err.message });
    return;
  }
  
  if (err.name === 'UnauthorizedError') {
    res.status(401).json({ error: 'Unauthorized' });
    return;
  }
  
  if (err.name === 'ForbiddenError') {
    res.status(403).json({ error: 'Forbidden' });
    return;
  }
  
  // Don't leak internal errors in production
  const isDevelopment = process.env.NODE_ENV === 'development';
  
  res.status(500).json({
    error: 'Internal server error',
    message: isDevelopment ? err.message : 'An unexpected error occurred',
    requestId: req.headers['x-request-id'] || 'unknown',
  });
}

export function requestIdMiddleware(req: Request, res: Response, next: NextFunction): void {
  const requestId = req.headers['x-request-id'] as string || crypto.randomUUID();
  req.headers['x-request-id'] = requestId;
  res.setHeader('X-Request-ID', requestId);
  next();
}

export function compressionMiddleware(req: Request, res: Response, next: NextFunction): void {
  // Express compression is handled by the compression middleware
  // This is just a placeholder for custom logic
  next();
}

export function healthCheckMiddleware(req: Request, res: Response, next: NextFunction): void {
  if (req.path === '/health') {
    res.json({ status: 'healthy', timestamp: new Date().toISOString() });
    return;
  }
  
  if (req.path === '/metrics') {
    // Prometheus metrics endpoint
    next();
    return;
  }
  
  next();
}