import { z } from 'zod';

const envSchema = z.object({
  NODE_ENV: z.enum(['development', 'production', 'test']).default('development'),
  PORT: z.coerce.number().default(3000),
  
  // Database
  DATABASE_URL: z.string().url(),
  DATABASE_POOL_SIZE: z.coerce.number().default(20),
  DATABASE_SSL: z.coerce.boolean().default(false),
  
  // Redis
  REDIS_URL: z.string().url(),
  
  // JWT
  JWT_ACCESS_SECRET: z.string().min(32),
  JWT_REFRESH_SECRET: z.string().min(32),
  JWT_ACCESS_EXPIRY: z.string().default('15m'),
  JWT_REFRESH_EXPIRY: z.string().default('7d'),
  JWT_ISSUER: z.string().default('kurostream'),
  JWT_AUDIENCE: z.string().default('kurostream-app'),
  
  // Firebase
  FIREBASE_PROJECT_ID: z.string(),
  FIREBASE_CLIENT_EMAIL: z.string().email(),
  FIREBASE_PRIVATE_KEY: z.string(),
  
  // Google Play Integrity
  GOOGLE_CLOUD_PROJECT_NUMBER: z.string(),
  GOOGLE_CLOUD_PROJECT_ID: z.string(),
  
  // Security
  BCRYPT_ROUNDS: z.coerce.number().default(12),
  SESSION_MAX_AGE: z.coerce.number().default(604800000), // 7 days
  DEVICE_SESSION_LIMIT: z.coerce.number().default(5),
  
  // Rate Limiting
  RATE_LIMIT_WINDOW_MS: z.coerce.number().default(900000), // 15 min
  RATE_LIMIT_MAX_REQUESTS: z.coerce.number().default(100),
  AUTH_RATE_LIMIT_MAX: z.coerce.number().default(10),
  
  // Logging
  LOG_LEVEL: z.enum(['error', 'warn', 'info', 'http', 'verbose', 'debug', 'silly']).default('info'),
  LOG_DIR: z.string().default('./logs'),
  
  // Metrics
  METRICS_ENABLED: z.coerce.boolean().default(true),
  METRICS_PORT: z.coerce.number().default(9090),
  
  // External APIs
  TRAKT_CLIENT_ID: z.string().optional(),
  TRAKT_CLIENT_SECRET: z.string().optional(),
  OPENSUBTITLES_API_KEY: z.string().optional(),
  REALDEBRID_API_KEY: z.string().optional(),
  TMDB_API_KEY: z.string().optional(),
  
  // App
  APP_URL: z.string().url().default('https://kuro-stream-tv.lovable.app'),
  APP_NAME: z.string().default('KuroStream'),
  
  // Security
  CORS_ORIGIN: z.string().default('https://kuro-stream-tv.lovable.app'),
  TRUSTED_PROXIES: z.string().default('127.0.0.1'),
});

export type Config = z.infer<typeof envSchema>;

let config: Config | null = null;

export function loadConfig(): Config {
  if (config) return config;
  
  const result = envSchema.safeParse(process.env);
  if (!result.success) {
    console.error('❌ Invalid environment configuration:');
    console.error(result.error.flatten().fieldErrors);
    process.exit(1);
  }
  
  config = result.data;
  return config;
}

export function getConfig(): Config {
  if (!config) {
    return loadConfig();
  }
  return config;
}