# KuroHub — Web App AGENTS.md

## Project Overview

KuroHub is the KuroStream web dashboard (Cloudflare Pages + Workers).
Stack: Vite, React 19, TanStack Router/Query, Tailwind CSS 4, Firebase, Cloudflare Workers (Nitro).

## Commands

- Install: `npm install`
- Dev server: `npm run dev`
- Build: `npm run build`
- Lint: `npm run lint`
- Deploy: `npm run deploy` (wrangler)
- Typecheck: `npx tsc --noEmit`

## Conventions

- TypeScript strict mode; path aliases via `vite-tsconfig-paths`.
- UI components in `components/`; pages in routes under `Kurostream web app/`.
- Keep `node_modules` on internal storage (`/root/.npm` cache); avoid `/sdcard` for heavy I/O.
- Firebase project: `kurostream13`. `firebase-mcp` needs `/root/.config/firebase-mcp/serviceAccount.json`.
- Cloudflare deploy uses `wrangler`; OAuth scope: `account:read account:write user:read`.

## Performance

- Use `npm run dev` for local dev; `npm run build` for production.
- Keep Termux alive: `termux-wake-lock` before long builds.
- Monitor RAM with `htop` or `free -h`.
- If memory is tight, close other Kilo sessions (each costs ~0.7-1.0 GB RSS).

## Quick Shortcuts

- `kh` → cd to this folder
- `kbuild` → not used here; use `npm run build`
## Kilo Skills & MCPs

This project uses Kilo skills for AI-assisted development:
- Skill location: `/root/.config/kilo/skills/kurohub/SKILL.md`
- Project MCP config: `.kilo/kilo.jsonc`
- Key MCPs: firebase, cloudflare, playwright, context7, parallel-search, duckduckgo, html-extractor

## Quick Reference

- Dev: `npm run dev`
- Build: `npm run build`
- Deploy: `npm run deploy`
- Typecheck: `npx tsc --noEmit`
- Firebase: `kurostream13`
