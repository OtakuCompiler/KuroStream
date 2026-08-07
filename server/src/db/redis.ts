import Redis from 'ioredis';
import { getConfig } from '../config';
import { logger } from '../utils/logger';

let redisClient: Redis | null = null;

export function getRedisClient(): Redis {
  if (redisClient) return redisClient;
  
  const config = getConfig();
  
  redisClient = new Redis(config.REDIS_URL, {
    maxRetriesPerRequest: 3,
    retryDelayOnFailover: 100,
    enableReadyCheck: true,
    lazyConnect: false,
    connectionName: 'kurostream-server',
  });
  
  redisClient.on('connect', () => {
    logger.info('Redis connected');
  });
  
  redisClient.on('error', (err) => {
    logger.error('Redis error', { error: err.message });
  });
  
  redisClient.on('close', () => {
    logger.warn('Redis connection closed');
  });
  
  redisClient.on('reconnecting', () => {
    logger.info('Redis reconnecting...');
  });
  
  return redisClient;
}

export async function closeRedis(): Promise<void> {
  if (redisClient) {
    await redisClient.quit();
    redisClient = null;
    logger.info('Redis connection closed');
  }
}

export function getRedis(): Redis {
  if (!redisClient) {
    return getRedisClient();
  }
  return redisClient;
}

// Cache helpers
export async function cacheGet<T>(key: string): Promise<T | null> {
  const redis = getRedis();
  const value = await redis.get(key);
  if (!value) return null;
  try {
    return JSON.parse(value) as T;
  } catch {
    return null;
  }
}

export async function cacheSet(key: string, value: unknown, ttlSeconds: number = 3600): Promise<void> {
  const redis = getRedis();
  await redis.set(key, JSON.stringify(value), 'EX', ttlSeconds);
}

export async function cacheDel(key: string): Promise<void> {
  const redis = getRedis();
  await redis.del(key);
}

export async function cacheDelPattern(pattern: string): Promise<void> {
  const redis = getRedis();
  const keys = await redis.keys(pattern);
  if (keys.length > 0) {
    await redis.del(...keys);
  }
}

// Rate limiting with Redis
export async function rateLimitCheck(
  key: string,
  limit: number,
  windowMs: number
): Promise<{ allowed: boolean; remaining: number; resetAt: number }> {
  const redis = getRedis();
  const keyPrefix = `ratelimit:${key}`;
  const windowSec = Math.ceil(windowMs / 1000);
  
  const current = await redis.incr(keyPrefix);
  
  if (current === 1) {
    await redis.expire(keyPrefix, windowSec);
  }
  
  const ttl = await redis.ttl(keyPrefix);
  const resetAt = Date.now() + (ttl > 0 ? ttl * 1000 : windowMs);
  
  return {
    allowed: current <= limit,
    remaining: Math.max(0, limit - current),
    resetAt,
  };
}

export async function rateLimitReset(key: string): Promise<void> {
  const redis = getRedis();
  await redis.del(`ratelimit:${key}`);
}

export async function blockKey(key: string, durationMs: number): Promise<void> {
  const redis = getRedis();
  const keyPrefix = `block:${key}`;
  await redis.set(keyPrefix, '1', 'PX', durationMs);
}

export async function isBlocked(key: string): Promise<boolean> {
  const redis = getRedis();
  const result = await redis.get(`block:${key}`);
  return result === '1';
}