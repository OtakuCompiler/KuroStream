export interface User {
  id: string;
  email: string;
  display_name: string | null;
  avatar_url: string | null;
  created_at: Date;
  updated_at: Date;
  last_login: Date | null;
  is_active: boolean;
  email_verified: boolean;
}

export interface UserProfile {
  id: string;
  user_id: string;
  display_name: string;
  avatar_url: string | null;
  pin_hash: string | null;
  is_kids_mode: boolean;
  parental_controls: ParentalControls | null;
  language: string;
  subtitle_language: string;
  subtitle_preferences: SubtitlePreferences;
  playback_preferences: PlaybackPreferences;
  active_skin_id: string | null;
  created_at: Date;
  updated_at: Date;
}

export interface ParentalControls {
  max_rating: string;
  block_unrated: boolean;
  require_pin_for_purchases: boolean;
  watch_time_limit_minutes: number | null;
  blocked_categories: string[];
}

export interface SubtitlePreferences {
  font_size: number;
  font_color: string;
  background_color: string;
  font_family: string;
  enabled: boolean;
}

export interface PlaybackPreferences {
  auto_play_next: boolean;
  skip_intro: boolean;
  skip_outro: boolean;
  default_quality: string;
  hardware_acceleration: boolean;
  background_playback: boolean;
}

export interface DeviceSession {
  id: string;
  user_id: string;
  profile_id: string | null;
  device_id: string;
  device_name: string;
  device_type: 'tv' | 'mobile' | 'desktop' | 'tablet';
  platform: string;
  app_version: string;
  push_token: string | null;
  last_active: Date;
  created_at: Date;
  revoked_at: Date | null;
}

export interface RefreshToken {
  id: string;
  user_id: string;
  token_hash: string;
  device_session_id: string | null;
  expires_at: Date;
  created_at: Date;
  revoked_at: Date | null;
}

export interface AccessTokenPayload {
  sub: string;
  email: string;
  profile_id: string | null;
  device_session_id: string | null;
  iat: number;
  exp: number;
  iss: string;
  aud: string;
  jti: string;
}

export interface Entitlements {
  owned_item_ids: string[];
  has_skins_pass: boolean;
  active_skin_id: string | null;
}

export interface Purchase {
  id: string;
  user_id: string;
  item_id: string;
  amount: number;
  currency: string;
  status: 'pending' | 'completed' | 'failed' | 'refunded';
  provider: string;
  provider_transaction_id: string | null;
  metadata: Record<string, unknown>;
  created_at: Date;
  completed_at: Date | null;
}

export interface CatalogItem {
  id: string;
  name: string;
  description: string | null;
  price: number;
  currency: string;
  skin_id: string | null;
  type: 'skin' | 'pass' | 'bundle';
  tier: 'free' | 'premium' | 'exclusive';
  preview_image_url: string | null;
  preview_video_url: string | null;
  metadata: Record<string, unknown>;
  is_active: boolean;
  sort_order: number;
  created_at: Date;
  updated_at: Date;
}

export interface ExtensionManifest {
  id: string;
  name: string;
  version: string;
  description: string;
  author: string;
  author_id: string;
  author_verified: boolean;
  type: 'source' | 'metadata' | 'subtitle' | 'utility';
  permissions: ExtensionPermission[];
  manifest_url: string;
  icon_url: string | null;
  screenshots: string[];
  categories: string[];
  supported_languages: string[];
  min_app_version: string;
  max_app_version: string | null;
  rating: number;
  review_count: number;
  download_count: number;
  status: 'pending' | 'approved' | 'rejected' | 'suspended';
  moderation_notes: string | null;
  created_at: Date;
  updated_at: Date;
  published_at: Date | null;
}

export interface ExtensionPermission {
  name: string;
  description: string;
  required: boolean;
}

export interface ExtensionInstallation {
  id: string;
  user_id: string;
  extension_id: string;
  version: string;
  installed_at: Date;
  updated_at: Date | null;
  is_enabled: boolean;
  config: Record<string, unknown>;
}

export interface WatchProgress {
  id: string;
  user_id: string;
  profile_id: string | null;
  media_id: string;
  media_type: 'movie' | 'series' | 'anime';
  episode_id: string | null;
  season: number | null;
  episode: number | null;
  position_ms: number;
  duration_ms: number;
  completed: boolean;
  watched_at: Date;
  updated_at: Date;
}

export interface Favorite {
  id: string;
  user_id: string;
  profile_id: string | null;
  media_id: string;
  media_type: 'movie' | 'series' | 'anime';
  added_at: Date;
}

export interface SyncConflict {
  id: string;
  user_id: string;
  entity_type: string;
  entity_id: string;
  local_version: Record<string, unknown>;
  remote_version: Record<string, unknown>;
  resolved_version: Record<string, unknown> | null;
  resolution_strategy: 'last_write_wins' | 'timestamp_merge' | 'device_priority' | 'manual';
  resolved_at: Date | null;
  created_at: Date;
}

export interface AuditLog {
  id: string;
  user_id: string | null;
  action: string;
  entity_type: string | null;
  entity_id: string | null;
  old_values: Record<string, unknown> | null;
  new_values: Record<string, unknown> | null;
  ip_address: string | null;
  user_agent: string | null;
  success: boolean;
  error_message: string | null;
  created_at: Date;
}

export interface RateLimitEntry {
  key: string;
  count: number;
  reset_at: Date;
  blocked_until: Date | null;
}

export interface DeviceTrustScore {
  device_id: string;
  score: number;
  factors: {
    root_detected: boolean;
    emulator_detected: boolean;
    tampered: boolean;
    debug_mode: boolean;
    vpn_detected: boolean;
    play_integrity_valid: boolean;
  };
  last_checked: Date;
}