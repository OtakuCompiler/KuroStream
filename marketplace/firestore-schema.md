# KuroStream Marketplace Firestore Schema
# Version 1.0

## Collection Structure

### users/{uid}
User document - created on first sign in via Firebase Auth.

```
uid: string (Firebase Auth UID)
email: string
displayName: string | null
photoUrl: string | null
createdAt: timestamp
lastLoginAt: timestamp
preferredPlatform: string (android_tv, fire_tv, webos, tizen, desktop, android)
notificationToken: string | null
settings: {
  emailNotifications: boolean
  marketingEmails: boolean
}
```

### users/{uid}/purchases/{productId}
Purchase records - ONLY written by Cloud Functions, never by clients.

```
productId: string (e.g., "skin_arctic_fuse_pro", "addon_adblock_plus")
productType: string ("skin" | "addon" | "subscription")
version: string (semver)
purchaseDate: timestamp
status: string ("active" | "refunded" | "expired" | "revoked")
downloadUrl: string (signed Cloud Storage URL)
checksum: string (SHA-256)
licenseVersion: number (for license key rotation)
platformCompatibility: string[] (["android_tv", "fire_tv", "webos", "tizen"])
lastUpdated: timestamp
expiresAt: timestamp | null (for subscriptions)
refundedAt: timestamp | null
refundReason: string | null
```

### users/{uid}/profiles/{profileId}
Multi-profile support for household sharing.

```
profileId: string
name: string
avatarUrl: string | null
isActive: boolean
createdAt: timestamp
skinPreferences: {
  activeSkinId: string
  skinSettings: map<string, any>
}
playbackSettings: {
  defaultQuality: string
  skipIntro: boolean
  skipOutro: boolean
}
```

### users/{uid}/syncMetadata
Cross-device sync tracking.

```
lastSyncAt: timestamp
syncVersion: number
pendingChanges: string[] (change IDs not yet synced)
deviceTokens: string[] (registered devices for push)
```

### products/{productId}
Product catalog - managed via Firebase Console or Admin SDK.

```
productId: string
name: string
description: string
longDescription: string
productType: string ("skin" | "addon")
category: string ("themes" | "extensions" | "utilities")
price: number (in cents)
currency: string ("usd")
images: string[] (screenshots, previews)
version: string
minAppVersion: string (semver)
platformCompatibility: string[]
downloadUrl: string
checksum: string
features: string[]
reviews: {
  averageRating: number
  totalReviews: number
}
isFeatured: boolean
isPremium: boolean
createdAt: timestamp
updatedAt: timestamp
```

### products/{productId}/reviews/{reviewId}
User reviews for products.

```
userId: string
userName: string
rating: number (1-5)
title: string
body: string
createdAt: timestamp
helpfulCount: number
```

### transactions/{transactionId}
Stripe payment transactions for auditing.

```
transactionId: string (Stripe PaymentIntent ID)
userId: string
productId: string
amount: number
currency: string
status: string ("pending" | "succeeded" | "failed" | "refunded")
paymentMethod: string
createdAt: timestamp
completedAt: timestamp | null
metadata: map<string, any>
```

## Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Helper functions
    function isAuthenticated() {
      return request.auth != null;
    }
    
    function isOwner(uid) {
      return request.auth.uid == uid;
    }
    
    function isValidProductType(type) {
      return type in ['skin', 'addon', 'subscription'];
    }
    
    // Users can read/write their own profile
    match /users/{uid} {
      allow read: if isAuthenticated() && isOwner(uid);
      allow write: if isAuthenticated() && isOwner(uid);
      
      // Purchases - ONLY Cloud Functions can write
      match /purchases/{productId} {
        allow read: if isAuthenticated() && isOwner(uid);
        // No allow write - Cloud Functions only
      }
      
      // Profiles
      match /profiles/{profileId} {
        allow read, write: if isAuthenticated() && isOwner(uid);
      }
      
      // Sync metadata
      match /syncMetadata {
        allow read, write: if isAuthenticated() && isOwner(uid);
      }
    }
    
    // Products catalog - anyone can read
    match /products/{productId} {
      allow read: if true;
      // No write - Admin SDK only
      
      match /reviews/{reviewId} {
        allow read: if true;
        allow create: if isAuthenticated();
        allow update: if isAuthenticated() && 
          resource.data.userId == request.auth.uid;
        allow delete: if isAuthenticated() && 
          resource.data.userId == request.auth.uid;
      }
    }
    
    // Transactions - Cloud Functions only
    match /transactions/{transactionId} {
      allow read: if isAuthenticated() && 
        resource.data.userId == request.auth.uid;
      // No write - Cloud Functions only
    }
  }
}
```