import { Migration } from './migrations';

export const initialSchema: Migration = {
  name: '001_initial_schema',
  checksum: 'a1b2c3d4e5f6g7h8i9j0',
  up: `
-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    avatar_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_login TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_active ON users(is_active) WHERE is_active = TRUE;

-- User profiles (multiple profiles per user)
CREATE TABLE user_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    display_name VARCHAR(100) NOT NULL,
    avatar_url TEXT,
    pin_hash VARCHAR(255),
    is_kids_mode BOOLEAN NOT NULL DEFAULT FALSE,
    parental_controls JSONB,
    language VARCHAR(10) NOT NULL DEFAULT 'en',
    subtitle_language VARCHAR(10) NOT NULL DEFAULT 'en',
    subtitle_preferences JSONB NOT NULL DEFAULT '{"font_size": 24, "font_color": "#FFFFFF", "background_color": "#80000000", "font_family": "default", "enabled": true}',
    playback_preferences JSONB NOT NULL DEFAULT '{"auto_play_next": true, "skip_intro": true, "skip_outro": true, "default_quality": "auto", "hardware_acceleration": true, "background_playback": false}',
    active_skin_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, display_name)
);

CREATE INDEX idx_user_profiles_user_id ON user_profiles(user_id);

-- Device sessions
CREATE TABLE device_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    profile_id UUID REFERENCES user_profiles(id) ON DELETE SET NULL,
    device_id VARCHAR(255) NOT NULL,
    device_name VARCHAR(255),
    device_type VARCHAR(20) NOT NULL CHECK (device_type IN ('tv', 'mobile', 'desktop', 'tablet')),
    platform VARCHAR(100),
    app_version VARCHAR(50),
    push_token TEXT,
    last_active TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMPTZ,
    UNIQUE(user_id, device_id)
);

CREATE INDEX idx_device_sessions_user_id ON device_sessions(user_id);
CREATE INDEX idx_device_sessions_active ON device_sessions(user_id, revoked_at) WHERE revoked_at IS NULL;

-- Refresh tokens
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    device_session_id UUID REFERENCES device_sessions(id) ON DELETE SET NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMPTZ
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at) WHERE revoked_at IS NULL;

-- Entitlements
CREATE TABLE entitlements (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    owned_item_ids TEXT[] NOT NULL DEFAULT '{}',
    has_skins_pass BOOLEAN NOT NULL DEFAULT FALSE,
    active_skin_id VARCHAR(100),
    last_synced TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Purchases
CREATE TABLE purchases (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    item_id VARCHAR(100) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL CHECK (status IN ('pending', 'completed', 'failed', 'refunded')),
    provider VARCHAR(50) NOT NULL,
    provider_transaction_id VARCHAR(255),
    metadata JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_purchases_user_id ON purchases(user_id);
CREATE INDEX idx_purchases_item_id ON purchases(item_id);
CREATE INDEX idx_purchases_status ON purchases(status);

-- Catalog items
CREATE TABLE catalog_items (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    skin_id VARCHAR(100),
    type VARCHAR(20) NOT NULL CHECK (type IN ('skin', 'pass', 'bundle')),
    tier VARCHAR(20) NOT NULL CHECK (tier IN ('free', 'premium', 'exclusive')),
    preview_image_url TEXT,
    preview_video_url TEXT,
    metadata JSONB NOT NULL DEFAULT '{}',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_catalog_active ON catalog_items(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_catalog_type ON catalog_items(type);
CREATE INDEX idx_catalog_tier ON catalog_items(tier);

-- Extensions
CREATE TABLE extensions (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    version VARCHAR(50) NOT NULL,
    description TEXT,
    author VARCHAR(255) NOT NULL,
    author_id VARCHAR(100) NOT NULL,
    author_verified BOOLEAN NOT NULL DEFAULT FALSE,
    type VARCHAR(20) NOT NULL CHECK (type IN ('source', 'metadata', 'subtitle', 'utility')),
    permissions JSONB NOT NULL DEFAULT '[]',
    manifest_url TEXT NOT NULL,
    icon_url TEXT,
    screenshots TEXT[] DEFAULT '{}',
    categories TEXT[] DEFAULT '{}',
    supported_languages TEXT[] DEFAULT '{}',
    min_app_version VARCHAR(50) NOT NULL,
    max_app_version VARCHAR(50),
    rating DECIMAL(3,2) DEFAULT 0,
    review_count INT DEFAULT 0,
    download_count BIGINT DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected', 'suspended')),
    moderation_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_extensions_status ON extensions(status);
CREATE INDEX idx_extensions_type ON extensions(type);
CREATE INDEX idx_extensions_author ON extensions(author_id);

-- Extension installations
CREATE TABLE extension_installations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    extension_id VARCHAR(100) NOT NULL REFERENCES extensions(id) ON DELETE CASCADE,
    version VARCHAR(50) NOT NULL,
    installed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    config JSONB NOT NULL DEFAULT '{}',
    UNIQUE(user_id, extension_id)
);

CREATE INDEX idx_ext_inst_user ON extension_installations(user_id);

-- Watch progress
CREATE TABLE watch_progress (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    profile_id UUID REFERENCES user_profiles(id) ON DELETE SET NULL,
    media_id VARCHAR(100) NOT NULL,
    media_type VARCHAR(20) NOT NULL CHECK (media_type IN ('movie', 'series', 'anime')),
    episode_id VARCHAR(100),
    season INT,
    episode INT,
    position_ms BIGINT NOT NULL DEFAULT 0,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    watched_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, media_id, episode_id)
);

CREATE INDEX idx_watch_progress_user ON watch_progress(user_id);
CREATE INDEX idx_watch_progress_media ON watch_progress(media_id, episode_id);

-- Favorites
CREATE TABLE favorites (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    profile_id UUID REFERENCES user_profiles(id) ON DELETE SET NULL,
    media_id VARCHAR(100) NOT NULL,
    media_type VARCHAR(20) NOT NULL CHECK (media_type IN ('movie', 'series', 'anime')),
    added_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, media_id)
);

CREATE INDEX idx_favorites_user ON favorites(user_id);

-- Sync conflicts
CREATE TABLE sync_conflicts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(255) NOT NULL,
    local_version JSONB NOT NULL,
    remote_version JSONB NOT NULL,
    resolved_version JSONB,
    resolution_strategy VARCHAR(50) NOT NULL DEFAULT 'last_write_wins' CHECK (resolution_strategy IN ('last_write_wins', 'timestamp_merge', 'device_priority', 'manual')),
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sync_conflicts_user ON sync_conflicts(user_id);
CREATE INDEX idx_sync_conflicts_entity ON sync_conflicts(entity_type, entity_id);
CREATE INDEX idx_sync_conflicts_unresolved ON sync_conflicts(resolved_at) WHERE resolved_at IS NULL;

-- Audit log
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id VARCHAR(255),
    old_values JSONB,
    new_values JSONB,
    ip_address INET,
    user_agent TEXT,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_created ON audit_logs(created_at);

-- Rate limiting
CREATE TABLE rate_limits (
    key VARCHAR(255) PRIMARY KEY,
    count INT NOT NULL DEFAULT 0,
    reset_at TIMESTAMPTZ NOT NULL,
    blocked_until TIMESTAMPTZ
);

CREATE INDEX idx_rate_limits_reset ON rate_limits(reset_at) WHERE blocked_until IS NULL;

-- Device trust scores
CREATE TABLE device_trust_scores (
    device_id VARCHAR(255) PRIMARY KEY,
    score INT NOT NULL DEFAULT 100,
    factors JSONB NOT NULL DEFAULT '{}',
    last_checked TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Triggers for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_user_profiles_updated_at BEFORE UPDATE ON user_profiles FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_catalog_items_updated_at BEFORE UPDATE ON catalog_items FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_extensions_updated_at BEFORE UPDATE ON extensions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_device_sessions_updated_at BEFORE UPDATE ON device_sessions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_extension_installations_updated_at BEFORE UPDATE ON extension_installations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_watch_progress_updated_at BEFORE UPDATE ON watch_progress FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
`
};