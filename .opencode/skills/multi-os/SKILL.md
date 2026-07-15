# Multi-OS Compilation Skill

KuroStream targets Android TV, LG webOS, Samsung Tizen from a single KMP codebase.

## Platform Targets
- **Android TV**: `app/` module, Compose for TV, Gradle build
- **webOS**: `webosApp/` module, Kotlin/JS, LG webOS TV SDK
- **Tizen**: `tizenApp/` module, Kotlin/JS, Samsung Tizen Studio

## Shared Code
- `domain/` — KMP module with commonMain, androidMain, jsMain
- Cross-platform interfaces in `domain/src/commonMain/`

## CI/CD (GitHub Actions)
- `ci.yml` — KMP compile + lint + debug build
- `code-quality.yml` — Detekt, Spotless, lint
- `deploy-preview.yml` — Preview deployment
- `nightly.yml` — Nightly builds
- `release.yml` — Release builds

## Build JS Targets
```bash
./gradlew :domain:compileKotlinJs :core-common:compileKotlinJs --no-daemon
```
