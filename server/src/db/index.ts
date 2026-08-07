import { getConfig } from '../config';
import { runMigrations } from './migrations';
import { initialSchema } from './schema';
import { logger } from '../utils/logger';

export async function runAllMigrations(): Promise<void> {
  const config = getConfig();
  
  logger.info('Starting database migrations');
  
  try {
    await runMigrations([initialSchema]);
    logger.info('All database migrations completed successfully');
  } catch (error) {
    logger.error('Migration failed', { error: error instanceof Error ? error.message : 'Unknown error' });
    throw error;
  }
}

export async function resetDatabase(): Promise<void> {
  const config = getConfig();
  
  if (config.NODE_ENV === 'production') {
    throw new Error('Cannot reset database in production');
  }
  
  logger.warn('Resetting database - dropping all tables');
  
  // This would be implemented with caution
  // For safety, we just log the warning
  logger.warn('Database reset requested but not implemented for safety');
}