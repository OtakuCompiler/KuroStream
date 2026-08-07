import { Pool, PoolConfig, PoolClient, QueryResult } from 'pg';
import { getConfig } from '../config';
import { logger } from '../utils/logger';

let pool: Pool | null = null;

export function getPool(): Pool {
  if (pool) return pool;
  
  const config = getConfig();
  
  const poolConfig: PoolConfig = {
    connectionString: config.DATABASE_URL,
    max: config.DATABASE_POOL_SIZE,
    idleTimeoutMillis: 30000,
    connectionTimeoutMillis: 5000,
    allowExitOnIdle: true,
  };
  
  if (config.DATABASE_SSL) {
    poolConfig.ssl = { rejectUnauthorized: false };
  }
  
  pool = new Pool(poolConfig);
  
  pool.on('error', (err) => {
    logger.error('Unexpected database pool error', { error: err.message });
  });
  
  pool.on('connect', (client) => {
    logger.debug('New database connection established');
  });
  
  return pool;
}

export async function query<T = unknown>(
  text: string,
  params?: unknown[]
): Promise<QueryResult<{ [key: string]: unknown }>> {
  const pool = getPool();
  const start = Date.now();
  
  try {
    const result = await pool.query(text, params);
    const duration = Date.now() - start;
    
    if (duration > 1000) {
      logger.warn('Slow query detected', { duration, text: text.substring(0, 100) });
    }
    
    return result as QueryResult<{ [key: string]: unknown }>;
  } catch (error) {
    logger.error('Database query failed', { 
      error: error instanceof Error ? error.message : 'Unknown error',
      text: text.substring(0, 200)
    });
    throw error;
  }
}

export async function getClient(): Promise<PoolClient> {
  const pool = getPool();
  const client = await pool.connect();
  
  const originalRelease = client.release.bind(client);
  client.release = () => {
    client.release = originalRelease;
    return originalRelease();
  };
  
  return client;
}

export async function transaction<T>(
  callback: (client: PoolClient) => Promise<T>
): Promise<T> {
  const client = await getClient();
  
  try {
    await client.query('BEGIN');
    const result = await callback(client);
    await client.query('COMMIT');
    return result;
  } catch (error) {
    await client.query('ROLLBACK');
    throw error;
  } finally {
    client.release();
  }
}

export async function closePool(): Promise<void> {
  if (pool) {
    await pool.end();
    pool = null;
    logger.info('Database pool closed');
  }
}

export function getPoolStats() {
  if (!pool) return null;
  return {
    totalCount: pool.totalCount,
    idleCount: pool.idleCount,
    waitingCount: pool.waitingCount,
  };
}