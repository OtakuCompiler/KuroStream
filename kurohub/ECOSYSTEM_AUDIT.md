# KuroStream Website — Audit & Cleanup Report

Scope of this pass: the **website only** (`kuro-stream-tv-firebase`), using the extension/skin catalog
from the marketplace bundle as source data. The Android app source and the raw marketplace bundle
were reviewed but not modified — see "Not included" below.

## What the marketplace bundle actually contained

30 extensions and 16 skins. On inspection:

- **9 extensions** were streaming-source aggregators with no licensing (movies, anime, sports, a
  torrent-streaming engine, and a "Comedy Central"-branded stream aggregator). These are excluded.
- **11 "premium" skins** were reskins of specific copyrighted anime characters (Naruto, Sasuke, Luffy,
  Ichigo, Gojo, Makima, Reze, Sukuna, Tanjiro, Madara, Aizen) sold at $2.99 each with no license.
  These are excluded.
- **21 extensions** (metadata, subtitles, sync, library tools, parental controls, cloud-drive
  playback, audio/UI enhancements, dev tools) are legitimate utility/integration features and are
  kept.
- **5 free skins** (color/aesthetic themes, no character references) are kept.

## Website findings (pre-existing, before this pass)

| Area | Finding |
|---|---|
| Marketing copy | Landing page, features page, FAQ, setup guide, and docs explicitly advertised "Stremio addons," "CloudStream plugins," and torrent streaming as core features. |
| Legal page | Terms-of-service boilerplate ("we do not host or index content, sources are provided by extensions") — the standard liability-deflection language used by piracy-adjacent apps. |
| Marketplace seed data | 3 of 5 seeded skins referenced specific franchises (Evangelion, Tokyo Ghoul, Cyberpunk Edgerunners) in their names/descriptions. |
| Database schema | `schema.sql` had a genuine SQL bug — `DEFAULT datetime('now')` is rejected by SQLite/D1 without wrapping parens (`DEFAULT (datetime('now'))`). This would have failed on real deployment, independent of any content issue. |
| Architecture | React 19 + TanStack Start + Cloudflare D1/KV + Firebase Auth. Marketplace, purchases, and entitlements are already properly data-driven from the DB via `/api/public/v1/*` routes — no hardcoded catalog in components. This made the cleanup straightforward. |

## Changes made

1. Rewrote `schema.sql` seed data: removed all franchise-referencing skins and piracy-framed
   copy; added 21 legitimate extensions and 7 new/reworked original skins as marketplace items.
2. Fixed the `DEFAULT datetime('now')` SQL syntax bug across all 4 tables.
3. Scrubbed torrent/Stremio/CloudStream references from: `Hero.tsx`, `BentoFeatures.tsx`,
   `FormatsMarquee.tsx`, `PerformanceStats.tsx`, `HowItWorks.tsx`, `FAQ.tsx`, `LegalBlock.tsx`,
   `features.tsx`, `setup.tsx`, `docs.tsx` — replaced with copy describing the real, legitimate
   feature set (cloud-drive playback, local library, metadata/subtitle/sync extensions).
4. Verified the marketplace, purchase, and entitlement components (`ExtCard`, `FeaturedSkins`,
   `marketplace.tsx`, `marketplace.$id.tsx`, `marketplace.ts`) are fully catalog-driven — no
   further code changes were needed there, only the underlying data.

## Not included in this pass

- **The Android app** (`kurostream.zip`) — untouched. It still contains a working torrent-streaming
  engine and a `TorrentSource` plugin interface. I didn't modify or build against this.
- **Payment processing / real entitlement enforcement** — the schema and API routes are structured
  for it, but no real payment provider (Stripe, etc.) is wired up; `file_url` is a placeholder `#`
  for every item. Wiring a real payment provider is a scoped follow-up, not something to fake.
- **npm install / production build** — this environment has no network access, so I could not run
  `npm install` or `npm run build` to get a compiler-verified pass. See the verification report for
  what was and wasn't checked.
