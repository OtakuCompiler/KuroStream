# Multi-stage Docker build for production
FROM node:20-alpine AS base

# Install dependencies
FROM base AS deps
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production

# Build TypeScript
FROM base AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Production image
FROM base AS runner
WORKDIR /app

ENV NODE_ENV=production

# Create non-root user
RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 kurostream

# Copy built application
COPY --from=builder --chown=kurostream:nodejs /app/dist ./dist
COPY --from=deps --chown=kurostream:nodejs /app/node_modules ./node_modules
COPY --chown=kurostream:nodejs /app/package.json ./package.json

USER kurostream

EXPOSE 3000

ENV PORT=3000
ENV NODE_ENV=production

CMD ["node", "dist/index.js"]