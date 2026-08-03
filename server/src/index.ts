import express from 'express';
import cors from 'cors';
import compression from 'compression';
import { metricsMiddleware, metricsEndpoint } from 'prom-client';
import { loadConfig } from './config';
import { runAllMigrations, closePool } from './db';
import { getRedisClient, closeRedis } from './db/redis';
import { logger, logRequest } from './utils/logger';
import { 
  corsMiddleware, 
  securityHeaders, 
  requestIdMiddleware, 
  healthCheckMiddleware,
  authMiddleware,
  errorHandler,
  createRateLimiter,
  createAuthRateLimiter,
} from './middleware';
import authRouter from './routes/auth';
import userRouter from './routes/users';
import profilesRouter from './routes/profiles';
import syncRouter from './routes/sync';
import marketplaceRouter from './routes/marketplace';
import notificationsRouter from './routes/notifications';
import extensionsRouter from './routes/extensions';
import healthRouter from './routes/health';

async function startServer() {
  // Load configuration
  const config = loadConfig();
  
  // Create Express app
  const app = express();
  
  // Trust proxy for correct IP detection behind load balancers
  app.set('trust proxy', config.TRUSTED_PROXIES.split(',').map(ip => ip.trim()));
  
  // Global middleware
  app.use(requestIdMiddleware);
  app.use(corsMiddleware);
  app.use(securityHeaders);
  app.use(compression());
  app.use(express.json({ limit: '10mb' }));
  app.use(express.urlencoded({ extended: true, limit: '10mb' }));
  app.use(logRequest);
  
  // Rate limiting
  const globalLimiter = createRateLimiter();
  const authLimiter = createAuthRateLimiter();
  app.use(globalLimiter);
  app.use('/api/auth/signin', authLimiter);
  app.use('/api/auth/signup', authLimiter);
  app.use('/api/auth/refresh', authLimiter);
  
  // Health checks (no rate limiting)
  app.use('/health', healthRouter);
  
  // Metrics endpoint
  app.get('/metrics', metricsEndpoint);
  
  // API routes
  const apiRouter = express.Router();
  apiRouter.use('/auth', authRouter);
  apiRouter.use('/users', userRouter);
  apiRouter.use('/profiles', profilesRouter);
  apiRouter.use('/sync', syncRouter);
  apiRouter.use('/marketplace', marketplaceRouter);
  apiRouter.use('/notifications', notificationsRouter);
  apiRouter.use('/extensions', extensionsRouter);
  
  app.use('/api/v1', apiRouter);
  
  // 404 handler
  app.use((req, res) => {
    res.status(404).json({ error: 'Not found', path: req.path });
  });
  
  // Error handler (must be last)
  app.use(errorHandler);
  
  // Initialize database and run migrations
  try {
    await runAllMigrations();
    logger.info('Database migrations completed');
  } catch (error) {
    logger.error('Failed to run migrations', { error: error instanceof Error ? error.message : 'Unknown' });
    process.exit(1);
  }
  
  // Initialize Redis
  getRedisClient();
  
  // Start server
  const server = app.listen(config.PORT, () => {
    logger.info(`Server started`, {
      port: config.PORT,
      environment: config.NODE_ENV,
      version: process.env.npm_package_version || '1.0.0',
    });
  });
  
  // Graceful shutdown
  const shutdown = async (signal: string) => {
    logger.info(`Received ${signal}, shutting down gracefully`);
    
    server.close(async () => {
      logger.info('HTTP server closed');
      
      try {
        await closePool();
        await closeRedis();
        logger.info('Connections closed');
        process.exit(0);
      } catch (error) {
        logger.error('Error during shutdown', { error: error instanceof Error ? error.message : 'Unknown' });
        process.exit(1);
      }
    });
    
    // Force close after 30 seconds
    setTimeout(() => {
      logger.error('Forced shutdown after timeout');
      process.exit(1);
    }, 30000);
  };
  
  process.on('SIGTERM', () => shutdown('SIGTERM'));
  process.on('SIGINT', () => shutdown('SIGINT'));
  
  process.on('unhandledRejection', (reason) => {
    logger.error('Unhandled rejection', { reason: reason instanceof Error ? reason.message : reason });
  });
  
  process.on('uncaughtException', (error) => {
    logger.error('Uncaught exception', { error: error.message, stack: error.stack });
    process.exit(1);
  });
}

startServer().catch((error) => {
  logger.error('Failed to start server', { error: error instanceof Error ? error.message : 'Unknown' });
  process.exit(1);
});