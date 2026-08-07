import { Router, Request, Response } from 'express';
import { getPool, getPoolStats, closePool } from '../db/pool';
import { getRedis, getRedisClient } from '../db/redis';
import { logger } from '../utils/logger';
import { getConfig } from '../config';

const router = Router();

router.get('/health', async (req: Request, res: Response) => {
  const start = Date.now();
  const checks: Record<string, { status: 'healthy' | 'unhealthy'; latency?: number; details?: any }> = {};
  
  // Database check
  try {
    const dbStart = Date.now();
    await query('SELECT 1');
    checks.database = {
      status: 'healthy',
      latency: Date.now() - dbStart,
      details: getPoolStats(),
    };
  } catch (error) {
    checks.database = {
      status: 'unhealthy',
      latency: Date.now() - start,
      details: { error: error instanceof Error ? error.message : 'Unknown' },
    };
  }
  
  // Redis check
  try {
    const redisStart = Date.now();
    const redis = getRedisClient();
    await redis.ping();
    checks.redis = {
      status: 'healthy',
      latency: Date.now() - redisStart,
    };
  } catch (error) {
    checks.redis = {
      status: 'unhealthy',
      details: { error: error instanceof Error ? error.message : 'Unknown' },
    };
  }
  
  // Overall health
  const allHealthy = Object.values(checks).every(c => c.status === 'healthy');
  const statusCode = allHealthy ? 200 : 503;
  
  res.status(statusCode).json({
    status: allHealthy ? 'healthy' : 'unhealthy',
    timestamp: new Date().toISOString(),
    version: process.env.npm_package_version || '1.0.0',
    environment: process.env.NODE_ENV || 'development',
    uptime: process.uptime(),
    checks,
  });
});

router.get('/health/live', (req, res) => {
  res.json({ status: 'alive', timestamp: new Date().toISOString() });
});

router.get('/health/ready', async (req, res) => {
  // Check if all critical dependencies are ready
  try {
    await query('SELECT 1');
    const redis = getRedisClient();
    await redis.ping();
    res.json({ ready: true });
  } catch (error) {
    res.status(503).json({ ready: false, error: 'Dependencies not ready' });
  }
});

export default router;