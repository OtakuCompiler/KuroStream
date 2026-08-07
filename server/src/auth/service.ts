import { query, transaction } from '../db/pool';
import { getConfig } from '../../config';
import { getRedis, cacheSet, cacheDel, rateLimitCheck, blockKey, isBlocked } from '../db/redis';
import {
  generateAccessToken,
  generateRefreshToken,
  verifyAccessToken,
  verifyRefreshToken,
  extractTokenFromHeader,
} from '../auth/jwt';
import { hashPassword, verifyPassword } from '../utils/crypto';
import { logger } from '../utils/logger';
import { generateDeviceId, generateSecureToken } from '../utils/helpers';
import { v4 as uuidv4 } from 'uuid';

export interface AuthResult {
  user: {
    id: string;
    email: string;
    display_name: string | null;
    avatar_url: string | null;
    email_verified: boolean;
  };
  profile: {
    id: string;
    display_name: string;
    is_kids_mode: boolean;
    parental_controls: any;
    language: string;
    subtitle_language: string;
    active_skin_id: string | null;
  } | null;
  tokens: {
    access_token: string;
    refresh_token: string;
    access_expires_at: Date;
    refresh_expires_at: Date;
  };
  device_session: {
    id: string;
    device_id: string;
    device_name: string;
    device_type: string;
  };
}

export interface SignUpInput {
  email: string;
  password: string;
  display_name?: string;
  device_id: string;
  device_name: string;
  device_type: 'tv' | 'mobile' | 'desktop' | 'tablet';
  platform?: string;
  app_version?: string;
  push_token?: string;
}

export interface SignInInput {
  email: string;
  password: string;
  device_id: string;
  device_name: string;
  device_type: 'tv' | 'mobile' | 'desktop' | 'tablet';
  platform?: string;
  app_version?: string;
  push_token?: string;
}

export interface RefreshTokenInput {
  refresh_token: string;
  device_id: string;
}

export interface DeviceSessionInfo {
  id: string;
  device_id: string;
  device_name: string;
  device_type: string;
  platform: string | null;
  app_version: string | null;
  last_active: Date;
  current: boolean;
}

class AuthService {
  private config = getConfig();
  
  async signUp(input: SignUpInput): Promise<AuthResult> {
    // Check if email already exists
    const existingUser = await query(
      'SELECT id FROM users WHERE email = $1',
      [input.email.toLowerCase()]
    );
    
    if (existingUser.rows.length > 0) {
      throw new Error('EMAIL_ALREADY_EXISTS');
    }
    
    // Check if device is blocked
    if (await isBlocked(`signup:${input.device_id}`)) {
      throw new Error('DEVICE_BLOCKED');
    }
    
    // Rate limit signup attempts
    const rateLimit = await rateLimitCheck(`signup:${input.email}`, 3, 3600000); // 3 per hour
    if (!rateLimit.allowed) {
      await blockKey(`signup:${input.device_id}`, 3600000); // Block for 1 hour
      throw new Error('RATE_LIMIT_EXCEEDED');
    }
    
    const passwordHash = await hashPassword(input.password);
    const userId = uuidv4();
    const profileId = uuidv4();
    const deviceSessionId = uuidv4();
    const deviceId = input.device_id;
    const displayName = input.display_name || input.email.split('@')[0];
    
    return await transaction(async (client) => {
      // Create user
      await client.query(
        `INSERT INTO users (id, email, password_hash, display_name, email_verified)
         VALUES ($1, $2, $3, $4, FALSE)
         RETURNING id, email, display_name, avatar_url, email_verified`,
        [userId, input.email.toLowerCase(), passwordHash, displayName]
      );
      
      // Create default profile
      await client.query(
        `INSERT INTO user_profiles (id, user_id, display_name, is_kids_mode)
         VALUES ($1, $2, $3, FALSE)`,
        [profileId, userId, displayName]
      );
      
      // Create device session
      await client.query(
        `INSERT INTO device_sessions (id, user_id, profile_id, device_id, device_name, device_type, platform, app_version, push_token)
         VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)`,
        [deviceSessionId, userId, profileId, input.device_id, input.device_name, input.device_type, input.platform || 'android', input.app_version || '1.0.0', input.push_token || null]
      );
      
      // Generate tokens
      const { token: accessToken, expiresAt: accessExpiresAt, jti: accessJti } = await generateAccessToken(
        userId,
        input.email.toLowerCase(),
        profileId,
        deviceSessionId
      );
      
      const { token: refreshToken, expiresAt: refreshExpiresAt, jti: refreshJti } = await generateRefreshToken(
        userId,
        deviceSessionId
      );
      
      // Store refresh token hash
      const refreshTokenHash = await hashPassword(refreshToken);
      await client.query(
        `INSERT INTO refresh_tokens (id, user_id, token_hash, device_session_id, expires_at)
         VALUES ($1, $2, $3, $4, $5)`,
        [uuidv4(), userId, refreshTokenHash, deviceSessionId, new Date(Date.now() + 7 * 24 * 60 * 60 * 1000)]
      );
      
      // Cache user data
      const redis = getRedis();
      await cacheSet(`user:${userId}`, {
        id: userId,
        email: input.email.toLowerCase(),
        display_name: displayName,
        avatar_url: null,
        email_verified: false,
      }, 3600);
      
      await cacheSet(`profile:${profileId}`, {
        id: profileId,
        display_name: displayName,
        is_kids_mode: false,
        parental_controls: null,
        language: 'en',
        subtitle_language: 'en',
        active_skin_id: null,
      }, 3600);
      
      // Log audit
      await this.logAudit(userId, 'signup', 'user', userId, null, { email: input.email });
      
      return {
        user: {
          id: userId,
          email: input.email.toLowerCase(),
          display_name: displayName,
          avatar_url: null,
          email_verified: false,
        },
        profile: {
          id: profileId,
          display_name: displayName,
          is_kids_mode: false,
          parental_controls: null,
          language: 'en',
          subtitle_language: 'en',
          active_skin_id: null,
        },
        tokens: {
          access_token: accessToken,
          refresh_token: refreshToken,
          access_expires_at: new Date(Date.now() + 15 * 60 * 1000),
          refresh_expires_at: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000),
        },
        device_session: {
          id: deviceSessionId,
          device_id: input.device_id,
          device_name: input.device_name,
          device_type: input.device_type,
        },
      };
    });
  }
  
  async signIn(input: SignInInput): Promise<AuthResult> {
    // Check if device is blocked
    if (await isBlocked(`signin:${input.device_id}`)) {
      throw new Error('DEVICE_BLOCKED');
    }
    
    // Rate limit signin attempts
    const rateLimit = await rateLimitCheck(`signin:${input.email}`, 5, 900000); // 5 per 15 min
    if (!rateLimit.allowed) {
      await blockKey(`signin:${input.device_id}`, 900000); // Block for 15 min
      throw new Error('RATE_LIMIT_EXCEEDED');
    }
    
    // Get user
    const userResult = await query(
      `SELECT id, email, password_hash, display_name, avatar_url, email_verified, is_active
       FROM users WHERE email = $1`,
      [input.email.toLowerCase()]
    );
    
    if (userResult.rows.length === 0) {
      // Don't reveal if email exists
      throw new Error('INVALID_CREDENTIALS');
    }
    
    const user = userResult.rows[0];
    
    if (!user.is_active) {
      throw new Error('ACCOUNT_DISABLED');
    }
    
    const validPassword = await verifyPassword(input.password, user.password_hash);
    if (!validPassword) {
      throw new Error('INVALID_CREDENTIALS');
    }
    
    // Get or create default profile
    let profileResult = await query(
      `SELECT id, display_name, is_kids_mode, parental_controls, language, subtitle_language, active_skin_id
       FROM user_profiles WHERE user_id = $1 ORDER BY created_at LIMIT 1`,
      [user.id]
    );
    
    let profileId: string;
    let profile: any;
    
    if (profileResult.rows.length === 0) {
      profileId = uuidv4();
      await query(
        `INSERT INTO user_profiles (id, user_id, display_name) VALUES ($1, $2, $3)`,
        [profileId, user.id, user.display_name || user.email.split('@')[0]]
      );
      profile = {
        id: profileId,
        display_name: user.display_name || user.email.split('@')[0],
        is_kids_mode: false,
        parental_controls: null,
        language: 'en',
        subtitle_language: 'en',
        active_skin_id: null,
      };
    } else {
      profile = profileResult.rows[0];
      profileId = profile.id;
    }
    
    // Create or update device session
    const deviceSessionId = uuidv4();
    const existingSession = await query(
      `SELECT id FROM device_sessions WHERE user_id = $1 AND device_id = $2 AND revoked_at IS NULL`,
      [user.id, input.device_id]
    );
    
    if (existingSession.rows.length > 0) {
      // Update existing session
      await query(
        `UPDATE device_sessions 
         SET profile_id = $1, device_name = $2, device_type = $3, platform = $4, app_version = $5, push_token = $6, last_active = NOW()
         WHERE id = $7`,
        [profileId, input.device_name, input.device_type, input.platform || 'android', input.app_version || '1.0.0', input.push_token || null, existingSession.rows[0].id]
      );
      return this.createTokensAndResponse(user, profile, existingSession.rows[0].id, input.device_id, input.device_name, input.device_type);
    } else {
      const deviceSessionId = uuidv4();
      await query(
        `INSERT INTO device_sessions (id, user_id, profile_id, device_id, device_name, device_type, platform, app_version, push_token)
         VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)`,
        [deviceSessionId, user.id, profileId, input.device_id, input.device_name, input.device_type, input.platform || 'android', input.app_version || '1.0.0', input.push_token || null]
      );
      return this.createTokensAndResponse(user, profile, deviceSessionId, input.device_id, input.device_name, input.device_type);
    }
  }
  
  private async createTokensAndResponse(
    user: any,
    profile: any,
    deviceSessionId: string,
    deviceId: string,
    deviceName: string,
    deviceType: string
  ): Promise<AuthResult> {
    const { token: accessToken, expiresAt: accessExpiresAt } = await generateAccessToken(
      user.id,
      user.email,
      profile.id,
      deviceSessionId
    );
    
    const { token: refreshToken, expiresAt: refreshExpiresAt } = await generateRefreshToken(
      user.id,
      deviceSessionId
    );
    
    const refreshTokenHash = await hashPassword(refreshToken);
    await query(
      `INSERT INTO refresh_tokens (id, user_id, token_hash, device_session_id, expires_at)
       VALUES ($1, $2, $3, $4, $5)`,
      [uuidv4(), user.id, refreshTokenHash, deviceSessionId, new Date(Date.now() + 7 * 24 * 60 * 60 * 1000)]
    );
    
    // Update last login
    await query(
      `UPDATE users SET last_login = NOW() WHERE id = $1`,
      [user.id]
    );
    
    // Log audit
    await this.logAudit(user.id, 'signin', 'user', user.id, null, { device_id: deviceId });
    
    return {
      user: {
        id: user.id,
        email: user.email,
        display_name: user.display_name,
        avatar_url: user.avatar_url,
        email_verified: user.email_verified,
      },
      profile: {
        id: profile.id,
        display_name: profile.display_name,
        is_kids_mode: profile.is_kids_mode,
        parental_controls: profile.parental_controls,
        language: profile.language,
        subtitle_language: profile.subtitle_language,
        active_skin_id: profile.active_skin_id,
      },
      tokens: {
        access_token: accessToken,
        refresh_token: refreshToken,
        access_expires_at: new Date(Date.now() + 15 * 60 * 1000),
        refresh_expires_at: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000),
      },
      device_session: {
        id: deviceSessionId,
        device_id: deviceId,
        device_name: deviceName,
        device_type: deviceType,
      },
    };
  }
  
  async refreshTokens(input: RefreshTokenInput): Promise<{ access_token: string; refresh_token: string; access_expires_at: Date; refresh_expires_at: Date }> {
    // Verify refresh token
    let payload: any;
    try {
      payload = await verifyRefreshToken(input.refresh_token);
    } catch {
      throw new Error('INVALID_REFRESH_TOKEN');
    }
    
    // Check if refresh token exists and is valid
    const tokenResult = await query(
      `SELECT id, token_hash, expires_at, revoked_at, device_session_id
       FROM refresh_tokens
       WHERE user_id = $1 AND device_session_id = (
         SELECT id FROM device_sessions WHERE device_id = $2 AND revoked_at IS NULL
       )`,
      [payload.sub, input.device_id]
    );
    
    if (tokenResult.rows.length === 0) {
      throw new Error('INVALID_REFRESH_TOKEN');
    }
    
    const tokenRecord = tokenResult.rows[0];
    
    if (tokenRecord.revoked_at) {
      throw new Error('TOKEN_REVOKED');
    }
    
    if (new Date(tokenRecord.expires_at) < new Date()) {
      throw new Error('TOKEN_EXPIRED');
    }
    
    // Verify token hash
    const validToken = await verifyPassword(input.refresh_token, tokenRecord.token_hash);
    if (!validToken) {
      throw new Error('INVALID_REFRESH_TOKEN');
    }
    
    // Revoke old refresh token
    await query(
      `UPDATE refresh_tokens SET revoked_at = NOW() WHERE id = $1`,
      [tokenRecord.id]
    );
    
    // Get user and profile
    const userResult = await query(
      `SELECT id, email, display_name, avatar_url, email_verified
       FROM users WHERE id = $1`,
      [payload.sub]
    );
    
    const user = userResult.rows[0];
    
    const profileResult = await query(
      `SELECT id, display_name, is_kids_mode, parental_controls, language, subtitle_language, active_skin_id
       FROM user_profiles WHERE user_id = $1 ORDER BY created_at LIMIT 1`,
      [user.id]
    );
    
    const profile = profileResult.rows[0] || { id: null, display_name: user.display_name };
    
    // Generate new tokens
    const { token: accessToken, expiresAt: accessExpiresAt } = await generateAccessToken(
      user.id,
      user.email,
      profile.id,
      tokenRecord.device_session_id
    );
    
    const { token: refreshToken, expiresAt: refreshExpiresAt } = await generateRefreshToken(
      user.id,
      tokenRecord.device_session_id
    );
    
    const refreshTokenHash = await hashPassword(refreshToken);
    await query(
      `INSERT INTO refresh_tokens (id, user_id, token_hash, device_session_id, expires_at)
       VALUES ($1, $2, $3, $4, $5)`,
      [uuidv4(), user.id, refreshTokenHash, tokenRecord.device_session_id, new Date(Date.now() + 7 * 24 * 60 * 60 * 1000)]
    );
    
    return {
      access_token: accessToken,
      refresh_token: refreshToken,
      access_expires_at: new Date(Date.now() + 15 * 60 * 1000),
      refresh_expires_at: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000),
    };
  }
  
  async revokeDeviceSession(userId: string, deviceSessionId: string): Promise<void> {
    await transaction(async (client) => {
      await client.query(
        `UPDATE device_sessions SET revoked_at = NOW() WHERE id = $1 AND user_id = $2`,
        [deviceSessionId, userId]
      );
      
      await client.query(
        `UPDATE refresh_tokens SET revoked_at = NOW() WHERE device_session_id = $1`,
        [deviceSessionId]
      );
    });
    
    await cacheDel(`user:${userId}`);
    await cacheDel(`profile:${deviceSessionId}`); // Profile cache uses device session id
    
    await this.logAudit(userId, 'revoke_session', 'device_session', deviceSessionId);
  }
  
  async revokeAllSessions(userId: string, exceptSessionId?: string): Promise<void> {
    await transaction(async (client) => {
      if (exceptSessionId) {
        await client.query(
          `UPDATE device_sessions SET revoked_at = NOW() WHERE user_id = $1 AND id != $2 AND revoked_at IS NULL`,
          [userId, exceptSessionId]
        );
        
        await client.query(
          `UPDATE refresh_tokens SET revoked_at = NOW() WHERE device_session_id IN (
            SELECT id FROM device_sessions WHERE user_id = $1 AND id != $2 AND revoked_at IS NULL
          )`,
          [userId, exceptSessionId]
        );
      } else {
        await client.query(
          `UPDATE device_sessions SET revoked_at = NOW() WHERE user_id = $1 AND revoked_at IS NULL`,
          [userId]
        );
        
        await client.query(
          `UPDATE refresh_tokens SET revoked_at = NOW() WHERE device_session_id IN (
            SELECT id FROM device_sessions WHERE user_id = $1 AND revoked_at IS NULL
          )`,
          [userId]
        );
      }
    });
    
    await cacheDel(`user:${userId}`);
    
    await this.logAudit(userId, 'revoke_all_sessions', 'user', userId);
  }
  
  async getDeviceSessions(userId: string): Promise<DeviceSessionInfo[]> {
    const result = await query(
      `SELECT id, device_id, device_name, device_type, platform, app_version, last_active, 
              CASE WHEN revoked_at IS NULL THEN true ELSE false END as current
       FROM device_sessions
       WHERE user_id = $1
       ORDER BY last_active DESC`,
      [userId]
    );
    
    return result.rows.map(row => ({
      id: row.id,
      device_id: row.device_id,
      device_name: row.device_name,
      device_type: row.device_type,
      platform: row.platform,
      app_version: row.app_version,
      last_active: row.last_active,
      current: row.current,
    }));
  }
  
  async changePassword(userId: string, currentPassword: string, newPassword: string): Promise<void> {
    const userResult = await query(
      `SELECT password_hash FROM users WHERE id = $1`,
      [userId]
    );
    
    if (userResult.rows.length === 0) {
      throw new Error('USER_NOT_FOUND');
    }
    
    const valid = await verifyPassword(currentPassword, userResult.rows[0].password_hash);
    if (!valid) {
      throw new Error('CURRENT_PASSWORD_INCORRECT');
    }
    
    const newHash = await hashPassword(newPassword);
    await query(
      `UPDATE users SET password_hash = $1, updated_at = NOW() WHERE id = $2`,
      [newHash, userId]
    );
    
    // Revoke all sessions except current (handled by caller)
    await this.revokeAllSessions(userId);
    
    await this.logAudit(userId, 'password_change', 'user', userId);
  }
  
  async setPin(userId: string, profileId: string, pin: string): Promise<void> {
    const pinHash = await hashPassword(pin);
    await query(
      `UPDATE user_profiles SET pin_hash = $1, updated_at = NOW() WHERE id = $2 AND user_id = $3`,
      [pinHash, profileId, userId]
    );
    
    await this.logAudit(userId, 'pin_set', 'profile', profileId);
  }
  
  async verifyPin(userId: string, profileId: string, pin: string): Promise<boolean> {
    const result = await query(
      `SELECT pin_hash FROM user_profiles WHERE id = $1 AND user_id = $2`,
      [profileId, userId]
    );
    
    if (result.rows.length === 0 || !result.rows[0].pin_hash) {
      return false;
    }
    
    return verifyPassword(pin, result.rows[0].pin_hash);
  }
  
  async updateProfile(userId: string, profileId: string, updates: Partial<{
    display_name: string;
    avatar_url: string;
    language: string;
    subtitle_language: string;
    parental_controls: any;
    active_skin_id: string;
  }>): Promise<void> {
    const setClause: string[] = [];
    const values: any[] = [];
    let paramIndex = 1;
    
    for (const [key, value] of Object.entries(updates)) {
      if (value !== undefined) {
        setClause.push(`${key} = $${paramIndex++}`);
        values.push(value);
      }
    }
    
    if (setClause.length === 0) return;
    
    setClause.push(`updated_at = NOW()`);
    values.push(userId);
    values.push(profileId);
    
    await query(
      `UPDATE user_profiles SET ${setClause.join(', ')} WHERE id = $${paramIndex} AND user_id = $${paramIndex + 1}`,
      values
    );
    
    // Invalidate cache
    await cacheDel(`profile:${profileId}`);
    
    await this.logAudit(userId, 'profile_update', 'profile', profileId, null, updates);
  }
  
  private async logAudit(
    userId: string,
    action: string,
    entityType: string,
    entityId: string,
    oldValues?: any,
    newValues?: any
  ): Promise<void> {
    try {
      await query(
        `INSERT INTO audit_logs (user_id, action, entity_type, entity_id, old_values, new_values)
         VALUES ($1, $2, $3, $4, $5, $6)`,
        [userId, action, entityType, entityId, oldValues ? JSON.stringify(oldValues) : null, newValues ? JSON.stringify(newValues) : null]
      );
    } catch (error) {
      logger.error('Failed to log audit', { error: error instanceof Error ? error.message : 'Unknown' });
    }
  }
}

export const authService = new AuthService();