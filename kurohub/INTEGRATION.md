# KuroStream Setup Guide

## 1. Firebase Setup

1. Go to [Firebase Console](https://console.firebase.google.com) and create a new project
2. Enable **Authentication**:
   - Email/Password provider
   - Google provider (add your domain to authorized domains)
3. Enable **Firestore Database** (for future use, currently using D1)
4. Go to Project Settings > General > Your apps > Web
5. Copy the config values to `.env`

## 2. Cloudflare Setup

### Create D1 Database
```bash
npx wrangler d1 create kuro-stream-db
# Copy the database_id into wrangler.jsonc
```

### Create KV Namespace
```bash
npx wrangler kv namespace create "KURO_KV"
# Copy the id into wrangler.jsonc
```

### Initialize Schema
```bash
npx wrangler d1 execute kuro-stream-db --local --file=./schema.sql
```

### Generate Types
```bash
npx wrangler types
```

## 3. Local Development

```bash
npm install
npm run dev
```

## 4. Deploy

```bash
npm run build
npx wrangler deploy
```

## 5. Set Secrets (if needed)

```bash
npx wrangler secret put FIREBASE_API_KEY
```

## Security Notes
- All API routes have rate limiting via KV
- CORS headers are set on all responses
- Security headers (HSTS, CSP, X-Frame-Options) are added by the server
- Firebase tokens should be verified server-side in production
