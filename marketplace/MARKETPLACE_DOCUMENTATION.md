# KuroStream Marketplace Architecture Documentation

## Overview

The KuroStream Marketplace is a premium ecosystem for selling optional skins and extensions. The core application remains completely free, with premium purchases only unlocking cosmetic themes and optional premium addons.

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           KUROSTREAM MARKETPLACE                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐        │
│  │   Marketplace    │    │   Cloud         │    │   Firestore     │        │
│  │   Website        │───▶│   Functions     │───▶│   Database      │        │
│  │   (Next.js)      │    │   (Stripe)      │    │   (Purchases)   │        │
│  └──────────────────┘    └──────────────────┘    └──────────────────┘        │
│           │                      │                       │                   │
│           │                      │                       │                   │
│           ▼                      ▼                       ▼                   │
│  ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐        │
│  │   Firebase       │    │   Stripe         │    │   Firebase      │        │
│  │   Auth           │    │   Checkout       │    │   Auth          │        │
│  └──────────────────┘    └──────────────────┘    └──────────────────┘        │
│                                                                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                              TV APPLICATIONS                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐        │
│  │   Android TV     │    │   LG webOS       │    │   Samsung Tizen  │        │
│  │   App            │◀──▶│   App            │◀──▶│   App            │        │
│  └────────┬─────────┘    └────────┬─────────┘    └────────┬─────────┘        │
│           │                        │                        │                   │
│           ▼                        ▼                        ▼                   │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                        Shared Business Logic                         │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │    │
│  │  │ SyncProvider│  │ SkinManager │  │ExtensionMgr │  │ Room DB     │  │    │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘  │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Component Responsibilities

### 1. Marketplace Website (Next.js)

**Purpose:** Companion website for browsing and purchasing products.

**Features:**
- Browse skins and premium extensions
- Search and filter products
- Preview skins with screenshots
- User authentication (Firebase Auth)
- Stripe Checkout integration
- View owned purchases
- Re-download purchases

**Tech Stack:**
- Next.js 14 (App Router)
- Tailwind CSS
- Firebase SDK
- Stripe.js
- Framer Motion

**Key Files:**
- `/src/app/page.tsx` - Main marketplace page
- `/src/components/` - UI components
- `/src/hooks/useAuth.ts` - Authentication hook
- `/src/hooks/useProducts.ts` - Product fetching hook
- `/src/lib/firebase.ts` - Firebase configuration

### 2. Cloud Functions (Firebase)

**Purpose:** Backend logic for purchases, subscriptions, and user management.

**Functions:**

| Function | Trigger | Purpose |
|----------|---------|---------|
| `onStripeCheckoutCompleted` | Stripe Webhook | Creates purchase record after payment |
| `createCheckoutSession` | HTTPS Call | Creates Stripe Checkout session |
| `getProducts` | HTTPS Call | Returns product catalog |
| `getUserPurchases` | HTTPS Call | Returns user's purchases |
| `verifyOwnership` | HTTPS Call | TV app purchase verification |
| `onUserCreated` | Auth Trigger | Creates user document |
| `onSubscriptionRenewed` | Pub/Sub | Handles subscription expiration |

**Security:**
- All purchase records written ONLY by Cloud Functions
- Clients can only READ their own data
- Stripe webhook signature verification
- Admin-only operations for refunds

### 3. Firestore Schema

**Collections:**

```
users/{uid}
├── purchases/{productId}     # Purchase records (Cloud Functions only)
├── profiles/{profileId}       # Multi-profile support
└── syncMetadata               # Cross-device sync tracking

products/{productId}
├── reviews/{reviewId}         # User reviews

transactions/{transactionId}   # Payment audit trail (Cloud Functions only)
```

**Security Rules:**
- Users can read/write their own profile
- Purchases are read-only for users (written by Cloud Functions)
- Products are publicly readable
- Transactions are read-only for users (written by Cloud Functions)

### 4. TV Application Components

#### SyncProvider

**Purpose:** Synchronizes purchases between Firestore and local Room database.

**Features:**
- Startup sync: Downloads purchases when user signs in
- Periodic sync: WorkManager job every 15 minutes
- Manual refresh: User-triggered from settings
- Offline cache: Local database as source of truth
- Conflict resolution: Firestore is authoritative

**Data Flow:**
```
App Start → SyncProvider.syncOnStartup() → Firestore → Room DB → UI Update
```

#### SkinManager

**Purpose:** Manages premium skin lifecycle.

**Features:**
- Download skin packages from signed URLs
- Checksum verification for integrity
- ZIP extraction to app storage
- Manifest parsing and validation
- Active skin persistence via DataStore
- Skin rollback on corruption
- Update support

**Installation Flow:**
```
Purchase Record → Download ZIP → Verify Checksum → Extract → Activate
```

**Storage Structure:**
```
/data/data/com.kurostream.app/files/skins/
├── skin_arctic_fuse_pro/
│   ├── manifest.json
│   ├── theme.json
│   ├── assets/
│   └── preview.png
└── skin_dark_mode/
    └── ...
```

#### ExtensionManager

**Purpose:** Manages premium extension enable/disable based on ownership.

**Features:**
- Ownership verification from local purchase cache
- Automatic enable if owned
- Graceful disable if not owned or refunded
- Never crashes - degrades gracefully
- Purchase dialog for non-owned extensions
- QR code generation for TV purchase flow

**State Machine:**
```
Extension State:
  - Registered: Extension is known to the system
  - Owned: User has purchased
  - Enabled: Extension is active
  - Disabled: Extension is inactive (but may still be owned)
```

## Purchase Flow

### Website Purchase

```
1. User signs into Marketplace (Firebase Auth)
2. User browses products
3. User clicks "Purchase"
4. createCheckoutSession Cloud Function creates Stripe session
5. User redirected to Stripe Checkout
6. Payment completes
7. Stripe sends webhook to onStripeCheckoutCompleted
8. Cloud Function creates purchase record in Firestore
9. User redirected to success page
```

### TV App Sync

```
1. User signs into TV app (Firebase Auth)
2. SyncProvider.syncOnStartup() triggered
3. Fetches purchases from Firestore
4. Updates local Room database
5. SkinManager.restorePurchasedSkins() called
6. ExtensionManager.restoreEnabledExtensions() called
7. UI updates to show owned products
```

### Restore Purchases (New Device)

```
1. User signs into new TV device
2. SyncProvider downloads all purchases
3. Room database updated
4. SkinManager installs any missing skins
5. ExtensionManager enables any previously enabled extensions
6. All products available automatically
```

## Security Model

### Client-Side Protection

- Firebase Auth tokens for authentication
- Firestore security rules enforce access control
- Purchase records can ONLY be created by Cloud Functions
- Clients cannot forge ownership

### Server-Side Protection

- Stripe webhook signature verification
- Admin SDK for product management
- Signed Cloud Storage URLs for downloads
- Checksum verification for downloaded assets

### Download Security

```
1. Cloud Storage generates signed URL (expires in 1 hour)
2. Client downloads from signed URL
3. SHA-256 checksum verified after download
4. If checksum fails, file is deleted and error shown
5. Installation only proceeds if checksum matches
```

## Offline Behavior

### When Offline

- Previously purchased skins remain available
- Previously enabled extensions remain enabled
- Local Room database is source of truth
- Sync resumes when network returns

### Sync on Reconnect

```
Network Available → SyncProvider.performSync() → 
  Firestore → Merge → Room DB Update → UI Update
```

## Error Handling

| Scenario | Handling |
|----------|----------|
| Network unavailable | Use local cache, retry on reconnect |
| Firestore unavailable | Use local cache, queue sync |
| Stripe timeout | Show error, allow retry |
| Corrupted download | Delete file, show error, allow retry |
| Checksum mismatch | Delete file, show error, allow retry |
| Version mismatch | Show upgrade prompt |
| Storage full | Show error, suggest cleanup |
| Auth expired | Prompt re-authentication |

## Multi-Platform Support

### Supported Platforms

- Android TV
- Google TV
- Fire TV
- LG webOS
- Samsung Tizen
- Future: Desktop, Mobile

### Platform-Specific Implementation

Each platform implements:
- Platform-specific installation logic
- Native UI integration
- Platform-specific capability detection

Shared across platforms:
- Same Firestore purchase records
- Same authentication
- Same business logic (SyncProvider, SkinManager, ExtensionManager)

## Room Schema

```sql
-- Purchases table
CREATE TABLE purchases (
    product_id TEXT PRIMARY KEY,
    product_type TEXT NOT NULL,
    version TEXT NOT NULL,
    purchase_date INTEGER NOT NULL,
    status TEXT NOT NULL,
    download_url TEXT NOT NULL,
    checksum TEXT NOT NULL,
    license_version INTEGER DEFAULT 1,
    platform_compatibility TEXT, -- JSON array
    last_sync INTEGER NOT NULL,
    sync_status TEXT DEFAULT 'synced',
    expires_at INTEGER,
    refunded_at INTEGER,
    refund_reason TEXT
);

-- Extension configs table
CREATE TABLE extension_configs (
    extension_id TEXT PRIMARY KEY,
    config_json TEXT DEFAULT '{}',
    is_enabled INTEGER DEFAULT 1,
    installed_at INTEGER NOT NULL
);
```

## Future Scalability Recommendations

1. **CDN for Assets:** Use Firebase Storage with CDN for faster downloads
2. **Product Versions:** Add version tracking for updates
3. **Subscription Billing:** Add subscription support with Stripe Billing
4. **Gift Cards:** Add gift card functionality
5. **Promotions:** Add discount codes and limited offers
6. **Analytics:** Add purchase analytics and metrics
7. **Reviews:** Add user reviews and ratings
8. **Recommendations:** Add ML-based product recommendations

## API Reference

### Cloud Functions

```typescript
// Create checkout session
functions.httpsCallable('createCheckoutSession')({ productId: string }): 
  { sessionId: string, url: string }

// Get user purchases
functions.httpsCallable('getUserPurchases')(): 
  Purchase[]

// Verify ownership (for TV app)
functions.httpsCallable('verifyOwnership')({ productId: string }): 
  { owned: boolean, version?: string, downloadUrl?: string }

// Get products
functions.httpsCallable('getProducts')({ productType?: string }): 
  Product[]
```

## Sequence Diagrams

### Purchase Flow

```
User          Website         Stripe       Cloud Functions    Firestore
  │               │               │               │               │
  │──Purchase──▶│               │               │               │
  │               │──Create Session──▶│               │               │
  │               │               │               │               │
  │◀──Redirect──│               │               │               │
  │               │               │               │               │
  │──────────────Checkout Complete──────────────▶│               │
  │               │               │               │──Write Purchase──▶│
  │               │               │               │               │
  │◀───────────────────Success URL─────────────────────────────│
```

### TV App Sync Flow

```
TV App         SyncProvider      Firestore         Room DB
  │                  │                │                │
  │──App Start──────▶│                │                │
  │                  │──Get Purchases──▶│                │
  │                  │◀──Purchases────│                │
  │                  │                │                │
  │                  │──Merge/Dedup──────────────────▶│
  │                  │                │                │
  │◀──Update UI──────│                │                │
```

## Environment Variables

### Cloud Functions

```
STRIPE_SECRET_KEY=sk_...
STRIPE_WEBHOOK_SECRET=whsec_...
```

### Website

```
NEXT_PUBLIC_FIREBASE_API_KEY=...
NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN=...
NEXT_PUBLIC_FIREBASE_PROJECT_ID=...
NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY=pk_...
```

### TV App (via Firebase Config)

```
google-services.json (auto-configured)
```