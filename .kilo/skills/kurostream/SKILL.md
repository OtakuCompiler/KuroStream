---
name: kurostream
description: KuroStream build & conventions. Use when building, compiling, or verifying this Android TV repo — the wrapper must run via `bash gradlew`, build outputs live in /root/.kurostream-build, and Room DAO SQL must use snake_case @ColumnInfo names.
---

# KuroStream Project Skill

Guidance for working in the KuroStream repo on the Termux + proot device.

## Build
- Always `bash gradlew <task>` (FUSE storage ignores exec bits). Shorthand: `kbuild`.
- Verify with `:data:kspDebugKotlin` → `:app:compileDebugKotlin` → `:app:assembleDebug`.
- Build outputs/caches are in `/root/.kurostream-build` and `/root/.kurostream-gradle`, not `build/`.
- Heaps are capped (Gradle 2048m / Kotlin 1536m); do not raise them.

## Room / KSP pitfalls
- DAO queries must use `@ColumnInfo` names (snake_case): `skin_id`, `created_at`, `item_id`.
- Every entity a DAO touches must be in `KuroStreamDatabase.kt` `entities=[...]`, else "no such table" or `error.NonExistentClass` KSP failures.
- Missing cross-module deps surface as Hilt `InjectProcessingStep ... NonExistentClass` errors — check the parameter type's module dependency (e.g. `:data` needs `implementation(project(":cache"))`).

## Firebase
- Project `kurostream13`; `firebase.json` at repo root; rules in `marketplace/`.
- `firebase-mcp` requires `/root/.config/firebase-mcp/serviceAccount.json` (Admin SDK, not the CI token).
