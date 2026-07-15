# Repository Report

## Overview
- **Name:** KuroStream
- **Owner:** OtakuCompiler
- **Visibility:** Private
- **Default Branch:** main
- **Remote:** https://github.com/OtakuCompiler/KuroStream.git

## Statistics
- **Commits:** 50+
- **Branches:** main, master
- **Modules:** 18 Gradle modules
- **Languages:** Kotlin, C++, Groovy, TypeScript (Kotlin/JS)
- **Build System:** Gradle (Kotlin DSL) with Version Catalog

## Module Inventory

| Module | Type | Platforms | Lines of Code (est.) |
|--------|------|-----------|---------------------|
| app | Android App | Android | ~5,000 |
| domain | KMP Library | JVM, JS, Android | ~2,000 |
| data | Android Library | Android | ~3,000 |
| common | Android Library | Android | ~1,500 |
| core-common | KMP Library | JVM, JS, Android | ~1,000 |
| core-platform | KMP Library | JVM, JS, Android, iOS | ~1,500 |
| playback | Android Library | Android | ~4,000 |
| plugin-sdk | Android Library | Android | ~800 |
| sync | KMP Library | JVM, Android, iOS | ~500 |
| backup | Android Library | Android | ~1,200 |
| benchmark | Android Library | Android | ~800 |
| extensions | Android Library | Android | ~2,000 |
| launcher | Android Library | Android | ~3,000 |
| torrent | Android Library | Android | ~1,500 |
| webosApp | KMP Library | JS | ~500 |
| tizenApp | KMP Library | JS | ~500 |
| lint-checks | Android Library | Android | ~300 |
| build-logic | Gradle Plugin | - | ~1,000 |

## Key Technologies
- **Kotlin Multiplatform** for shared logic
- **Jetpack Compose** for UI
- **Hilt** for DI
- **Room** for database
- **Media3 (ExoPlayer)** for playback
- **Gradle Convention Plugins** in build-logic/
- **Version Catalog** in gradle/libs.versions.toml

## GitHub Features Enabled
- ✅ Issues
- ✅ Wiki
- ⚠️ Projects (not configured)
- ⚠️ Discussions (not configured)
- ✅ Actions (5 workflows)
- ✅ Dependabot (not configured)
- ✅ CodeQL (not configured)

## CI/CD Workflows
1. `ci.yml` - Main CI pipeline
2. `code-quality.yml` - Lint, Detekt, Spotless
3. `deploy-preview.yml` - Preview deployments
4. `nightly.yml` - Nightly builds
5. `release.yml` - Release automation