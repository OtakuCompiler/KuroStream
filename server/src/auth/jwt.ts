import { SignJWT, jwtVerify, JWTPayload } from 'jose';
import { getConfig } from '../config';
import { logger } from '../utils/logger';

const TOKEN_TYPE = {
  ACCESS: 'access',
  REFRESH: 'refresh',
  DEVICE: 'device',
  RESET: 'reset',
} as const;

type TokenType = (typeof TOKEN_TYPE)[keyof typeof TOKEN_TYPE];

interface TokenPayload extends JWTPayload {
  sub: string;
  email: string;
  profile_id?: string;
  device_session_id?: string;
  type: TokenType;
  jti: string;
}

export async function generateAccessToken(
  userId: string,
  email: string,
  profileId?: string,
  deviceSessionId?: string
): Promise<{ token: string; expiresAt: Date; jti: string }> {
  const config = getConfig();
  const jti = crypto.randomUUID();
  const expiresIn = config.JWT_ACCESS_EXPIRY;
  const expiresAt = new Date(Date.now() + parseDuration(expiresIn));
  
  const payload: TokenPayload = {
    sub: userId,
    email,
    profile_id: profileId,
    device_session_id: deviceSessionId,
    type: TOKEN_TYPE.ACCESS,
    jti,
    iss: config.JWT_ISSUER,
    aud: config.JWT_AUDIENCE,
  };
  
  const secret = new TextEncoder().encode(config.JWT_ACCESS_SECRET);
  const token = await new SignJWT(payload)
    .setProtectedHeader({ alg: 'HS256', typ: 'JWT' })
    .setIssuedAt()
    .setExpirationTime(expiresIn)
    .setJti(jti)
    .sign(secret);
  
  return { token, expiresAt, jti };
}

export async function generateRefreshToken(
  userId: string,
  deviceSessionId?: string
): Promise<{ token: string; expiresAt: Date; jti: string }> {
  const config = getConfig();
  const jti = crypto.randomUUID();
  const expiresIn = config.JWT_REFRESH_EXPIRY;
  const expiresAt = new Date(Date.now() + parseDuration(expiresIn));
  
  const payload: TokenPayload = {
    sub: userId,
    email: '',
    device_session_id: deviceSessionId,
    type: TOKEN_TYPE.REFRESH,
    jti,
    iss: config.JWT_ISSUER,
    aud: config.JWT_AUDIENCE,
  };
  
  const secret = new TextEncoder().encode(config.JWT_REFRESH_SECRET);
  const token = await new SignJWT(payload)
    .setProtectedHeader({ alg: 'HS256', typ: 'JWT' })
    .setIssuedAt()
    .setExpirationTime(expiresIn)
    .setJti(jti)
    .sign(secret);
  
  return { token, expiresAt, jti };
}

export async function generateDeviceToken(
  deviceId: string,
  userId: string
): Promise<{ token: string; expiresAt: Date; jti: string }> {
  const config = getConfig();
  const jti = crypto.randomUUID();
  const expiresIn = '365d'; // Device tokens last a year
  const expiresAt = new Date(Date.now() + parseDuration(expiresIn));
  
  const payload: TokenPayload = {
    sub: userId,
    email: '',
    device_session_id: deviceId,
    type: TOKEN_TYPE.DEVICE,
    jti,
    iss: config.JWT_ISSUER,
    aud: config.JWT_AUDIENCE,
  };
  
  const secret = new TextEncoder().encode(config.JWT_ACCESS_SECRET);
  const token = await new SignJWT(payload)
    .setProtectedHeader({ alg: 'HS256', typ: 'JWT' })
    .setIssuedAt()
    .setExpirationTime(expiresIn)
    .setJti(jti)
    .sign(secret);
  
  return { token, expiresAt, jti };
}

export async function verifyAccessToken(token: string): Promise<TokenPayload> {
  const config = getConfig();
  const secret = new TextEncoder().encode(config.JWT_ACCESS_SECRET);
  
  try {
    const { payload } = await jwtVerify(token, secret, {
      issuer: config.JWT_ISSUER,
      audience: config.JWT_AUDIENCE,
    });
    
    if (payload.type !== TOKEN_TYPE.ACCESS) {
      throw new Error('Invalid token type');
    }
    
    return payload as TokenPayload;
  } catch (error) {
    logger.warn('Access token verification failed', { error: error instanceof Error ? error.message : 'Unknown' });
    throw new Error('Invalid or expired access token');
  }
}

export async function verifyRefreshToken(token: string): Promise<TokenPayload> {
  const config = getConfig();
  const secret = new TextEncoder().encode(config.JWT_REFRESH_SECRET);
  
  try {
    const { payload } = await jwtVerify(token, secret, {
      issuer: config.JWT_ISSUER,
      audience: config.JWT_AUDIENCE,
    });
    
    if (payload.type !== TOKEN_TYPE.REFRESH) {
      throw new Error('Invalid token type');
    }
    
    return payload as TokenPayload;
  } catch (error) {
    logger.warn('Refresh token verification failed', { error: error instanceof Error ? error.message : 'Unknown' });
    throw new Error('Invalid or expired refresh token');
  }
}

export async function verifyDeviceToken(token: string): Promise<TokenPayload> {
  const config = getConfig();
  const secret = new TextEncoder().encode(config.JWT_ACCESS_SECRET);
  
  try {
    const { payload } = await jwtVerify(token, secret, {
      issuer: config.JWT_ISSUER,
      audience: config.JWT_AUDIENCE,
    });
    
    if (payload.type !== TOKEN_TYPE.DEVICE) {
      throw new Error('Invalid token type');
    }
    
    return payload as TokenPayload;
  } catch (error) {
    logger.warn('Device token verification failed', { error: error instanceof Error ? error.message : 'Unknown' });
    throw new Error('Invalid device token');
  }
}

function parseDuration(duration: string): number {
  const match = duration.match(/^(\d+)([smhd])$/);
  if (!match) return 15 * 60 * 1000; // default 15 minutes
  
  const value = parseInt(match[1], 10);
  const unit = match[2];
  
  switch (unit) {
    case 's': return value * 1000;
    case 'm': return value * 60 * 1000;
    case 'h': return value * 60 * 60 * 1000;
    case 'd': return value * 24 * 60 * 60 * 1000;
    default: return 15 * 60 * 1000;
  }
}

export async function decodeToken(token: string): Promise<TokenPayload | null> {
  try {
    const config = getConfig();
    const secret = new TextEncoder().encode(config.JWT_ACCESS_SECRET);
    const { payload } = await jwtVerify(token, secret, {
      issuer: config.JWT_ISSUER,
      audience: config.JWT_AUDIENCE,
    });
    return payload as TokenPayload;
  } catch {
    return null;
  }
}

export function extractTokenFromHeader(authHeader: string | undefined): string | null {
  if (!authHeader) return null;
  const parts = authHeader.split(' ');
  if (parts.length !== 2 || parts[0] !== 'Bearer') return null;
  return parts[1];
}