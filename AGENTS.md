# AGENTS.md — KuroStream Project Instructions

Instructions for AI agents working in this repository. Loaded automatically in every session.

## Project Overview

KuroStream is a privacy-focused Android TV streaming app (Kotlin, Jetpack Compose, Material 3, Hilt, Room/KSP, Media3). Multi-module Gradle project; domain layer is Kotlin Multiplatform. The repo lives at `/mnt/sdcard/kurostream` on an Android device running Termux + proot Ubuntu (ARM64).

## Build Commands (IMPORTANT)

- Always invoke the wrapper via bash: `bash gradlew <task>` — the repo is on FUSE storage (`/sdcard`), so the wrapper's exec bit is ignored and `./gradlew` fails with "Permission denied".
- Shorthand helper: `kbuild <task>` (defined in proot `~/.bashrc` → `bash gradlew --project-cache-dir /root/.kurostream-gradle "$@"`).
- Typical verification: `bash gradlew :data:kspDebugKotlin` then `bash gradlew :app:compileDebugKotlin`; full APK: `bash gradlew :app:assembleDebug`.
- Build outputs and Gradle caches live on internal ext4 storage (`/root/.kurostream-build/<module>`, `/root/.kurostream-gradle`) — NOT under `build/` in the repo. Override with env `KURO_BUILD_DIR`.
- Config cache + build cache + parallel workers are enabled. Heaps are capped (Gradle 2048m, Kotlin daemon 1536m) because the device has 5.5 GB RAM; never raise them above ~2.5 GB or the device OOM-kills the build.
- `oom-guard.sh` (background watchdog) kills stale Gradle/Kotlin daemons when free memory drops below 250 MB.
- `org.gradle.vfs.watch=false` is intentional (FUSE inotify is expensive). `android.aapt2daemonMode=outofprocess` is intentional (in-process daemons fail under proot parallelism).
- AAPT2 daemon "Daemon startup failed" errors under 6 parallel workers are a proot race; if they reappear, re-run the task.

## Architecture & Conventions

- **Modules:** `app` (TV UI/theme/sync), `playback` (KuroVision engine), `data` (repositories, Trakt/OpenSubtitles APIs, Room DB, subtitle engine, KuroCloud sync), `domain` (KMP models/usecases), `cache` (namespaced disk cache), `torrent`, `extensions`, `marketplace` (addons + Firebase backend), `plugin-sdk`, `common`, `config`, `ui`.
- **DI:** Hilt (`@Inject` constructors, `@Module`/`@InstallIn(SingletonComponent)`, `@Provides`+`@Singleton` for repositories/interfaces). Multibindings via `@IntoSet` (e.g. subtitle providers).
- **Persistence:** Room with KSP. Entities use snake_case `@ColumnInfo` names; DAO SQL must use the **column names**, not property names (e.g. `skin_id`, `created_at`, `item_id`). Every `@Entity` referenced by a DAO must be registered in the `@Database(entities=[...])` list in `KuroStreamDatabase.kt`, or Room fails with "no such table" / KSP `NonExistentClass` errors.
- **Serialization:** kotlinx.serialization with `@Serializable` for network models.
- **Networking:** Retrofit + OkHttp (coroutines-based suspend APIs).
- **Logging:** Timber. No analytics/telemetry anywhere.
- **Singleton managers** (TraktSyncManager, SubtitleDownloadManager) use `CoroutineScope(Dispatchers.IO + SupervisorJob)`.
- **Credentials:** Trakt client_id/client_secret and OpenSubtitles API key are hardcoded placeholders (`YOUR_TRAKT_CLIENT_ID`, etc.) in repository code. Firebase CI token is sourced from `/root/.local/config/firebase-token` (proot) and `~/.firebase-token` (Termux). Never commit real secrets.
- **License headers:** source files carry a GPL-3.0-only SPDX header — keep them on new files.
- **Code style:** match existing patterns; no kdoc/comments unless they add value; no emojis in code; follow the surrounding file's formatting.

## Firebase

- Default project: `kurostream13`. Deploy config at repo root: `firebase.json` + `.firebaserc`; rules in `marketplace/firestore.rules` and `marketplace/storage.rules` (public read, authenticated write).
- Deploy: `firebase deploy` from repo root (needs `FIREBASE_TOKEN` or `firebase login`). Functions require `npm install` in `marketplace/functions` first.
- The `firebase-mcp` server in `kilo.json` is Admin-SDK based and needs `/root/.config/firebase-mcp/serviceAccount.json` (service account key) — it cannot use the interactive login token.

## Device Environment (Termux + proot Ubuntu)

- proot Ubuntu auto-logs-in when Termux starts; `kd` = `cd /sdcard/kurostream`.
- `/sdcard` is FUSE: no exec bits, no symlinks (`ln -s` fails with "Permission denied").
- Performance governor is locked to `performance`; CPU affinity limits the build to 6 of 8 cores.
- Kilo itself runs on the Termux side (`kilo` is not on PATH inside proot root).
- Prefer internal-storage paths (`/root/...`) for anything performance-sensitive; keep source in the repo.

## Kilo Skills & MCPs

This project uses Kilo skills for AI-assisted development:
- Skill location: `/root/.config/kilo/skills/kurostream/SKILL.md`
- Global MCPs configured in `~/.config/kilo/kilo.jsonc`
- Key MCPs: firebase, cloudflare, playwright, context7, parallel-search, duckduckgo, html-extractor

## Quick Reference

- Always use `bash gradlew` instead of `./gradlew`
- Build outputs: `/root/.kurostream-build/`
- Gradle cache: `/root/.kurostream-gradle/`
- Firebase: `kurostream13`
