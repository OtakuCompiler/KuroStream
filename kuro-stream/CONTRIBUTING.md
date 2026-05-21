# Contributing to Kuro Stream

Thank you for your interest in contributing! This document explains how to get involved, the standards we follow, and how the project is structured so you can hit the ground running.

---

## Table of Contents

1. [Code of Conduct](#code-of-conduct)
2. [Getting Started](#getting-started)
3. [How to Contribute](#how-to-contribute)
4. [Coding Standards](#coding-standards)
5. [Architecture Guidelines](#architecture-guidelines)
6. [TV / D-pad Rules](#tv--d-pad-rules)
7. [Memory Optimisation Rules](#memory-optimisation-rules)
8. [Testing](#testing)
9. [Pull Request Process](#pull-request-process)
10. [Release Process](#release-process)

---

## Code of Conduct

Be respectful, constructive, and inclusive. We follow the [Contributor Covenant](https://www.contributor-covenant.org/) v2.1.

---

## Getting Started

### Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or later
- **JDK 17** (the Gradle toolchain will download it automatically if missing)
- A **Fire TV** or Android TV device / emulator for testing
  - Recommended emulator: Android TV API 34, 1024 MB RAM to simulate target hardware

### Setup

```bash
git clone https://github.com/your-org/kuro-stream.git
cd kuro-stream
./gradlew assembleDebug   # verify the build passes before you start
```

### IDE Setup

1. Open the `kuro-stream/` folder (not the repo root) in Android Studio.
2. Let Gradle sync complete.
3. Run on a connected Fire TV via **Run > Run 'app'** or `adb install`.

---

## How to Contribute

### Bugs

1. Search [existing issues](../../issues) — the bug may already be tracked.
2. Open a new issue with:
   - Device model + RAM + Android version
   - Steps to reproduce
   - Expected vs actual behaviour
   - Logcat output (filter by `KuroStream` tag)

### Features

1. Open an issue describing the feature and its use case **before** writing code. This avoids duplicated effort.
2. Wait for maintainer sign-off on the approach.
3. Implement in a feature branch and open a PR.

### Provider Integrations

New streaming providers must implement `AnimeProvider` and be registered in `ProviderAggregator`. Include tests that stub network responses.

---

## Coding Standards

### Kotlin Style

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Use `ktlint` (run `./gradlew ktlintCheck`).
- Max line length: **120 characters**.
- Prefer `val` over `var`.
- Prefer coroutines + `Flow` over callbacks.
- No `!!` — use `?.let`, `?:`, or `requireNotNull` with a descriptive message.

### Naming

| Element | Convention | Example |
|---|---|---|
| Classes | `PascalCase` | `SmartPlayerRouter` |
| Functions | `camelCase` | `routeStream()` |
| Constants | `SCREAMING_SNAKE` | `MAX_BUFFER_SIZE` |
| Composables | `PascalCase` | `HomeScreen()` |
| State holders | `*UiState` suffix | `PlayerUiState` |
| ViewModels | `*ViewModel` suffix | `PlayerViewModel` |

### Logging

- **Never use `android.util.Log` directly.** Use `timber.log.Timber`.
- Debug logs are stripped in release builds via ProGuard (`-assumenosideeffects`).

```kotlin
// Good
Timber.tag(TAG).d("Loading streams for: %s", animeId)
Timber.tag(TAG).e(e, "Failed to load streams")

// Bad
Log.d(TAG, "Loading streams for: $animeId")
```

### Coroutines

- Inject `CoroutineDispatcher` via Hilt — never hardcode `Dispatchers.IO` in business logic.
- Use `viewModelScope` in ViewModels, `SupervisorJob + ioDispatcher` in singletons.
- Emit errors via sealed `Result` types, not exceptions crossing layer boundaries.

---

## Architecture Guidelines

The project uses **Clean Architecture** + **MVVM** + **Hilt DI**. Every change must respect layer boundaries:

```
UI (Compose) → ViewModel → Domain (interfaces) → Data (implementations)
```

| Layer | Package | Rules |
|---|---|---|
| **UI** | `ui.*` | Compose only. Observes `StateFlow`. No direct data source access. |
| **ViewModel** | `ui.*.ViewModel` | Business logic coordination. `HiltViewModel`. Exposes `StateFlow<UiState>`. |
| **Domain** | `domain.*` | Pure Kotlin. No Android imports. Interfaces only — no implementations. |
| **Data** | `data.*` | Implements domain interfaces. Can import Android/Room/Retrofit. |
| **Core** | `core.*` | Cross-cutting utilities (player, performance). Android imports allowed. |
| **DI** | `di.*` | Hilt modules only. No business logic. |

**Anti-patterns to avoid:**

- Importing `data.*` classes directly in UI layer.
- Calling `suspend` functions from Composables — use `LaunchedEffect` or ViewModel.
- Exposing `MutableStateFlow` outside a ViewModel.
- Using `GlobalScope`.

---

## TV / D-pad Rules

All screens must be fully operable with a D-pad (no touch required):

1. **Focus management** — every interactive element must be focusable. Use `Modifier.focusable()` or TV Material components (`TvLazyColumn`, `TvCard`, etc.) which handle focus automatically.
2. **Focus restoration** — when navigating back to a screen, the previously focused item must regain focus. Use `rememberSaveable` focus requesters.
3. **No hover-only affordances** — every action accessible on hover must also be accessible via D-pad select.
4. **Key events** — `KeyEvent.KEYCODE_DPAD_*` and `KeyEvent.KEYCODE_MEDIA_*` must be handled in the player screen.
5. **Focus indicators** — use the TV Material `FocusHighlight` scale animation, not custom colour changes, for consistency.
6. **Test with remote** — before submitting a UI PR, verify the entire flow with only a D-pad. Mouse / touch testing is not sufficient.

---

## Memory Optimisation Rules

Kuro Stream targets 1 GB RAM Fire TV devices. Every PR touching media, images, or caching must comply:

- **ExoPlayer buffer**: do not exceed `minBufferMs = 8_000`, `maxBufferMs = 8_000`.
- **Image caches**: Coil disk + memory cache combined must not exceed 50 MB on low-end devices. Check `AppPerformanceMonitor.isLowEndDevice()`.
- **Bitmaps**: avoid loading full-resolution posters into memory. Use Coil's `size()` to request the display size.
- **Background work**: cancel coroutine scopes in `onCleared()` / `release()`.
- **Respond to `onTrimMemory`**: clear non-essential caches at `TRIM_MEMORY_RUNNING_LOW` or higher.
- **No static Bitmap references**: they prevent garbage collection.

---

## Testing

### Unit Tests

- Location: `app/src/test/`
- Framework: JUnit 4 + Mockk
- Required for: ViewModels, use cases, repository implementations, error handlers
- Mock coroutine dispatchers with `TestCoroutineDispatcher` / `StandardTestDispatcher`

```bash
./gradlew testDebugUnitTest
```

### Instrumented Tests

- Location: `app/src/androidTest/`
- Framework: Espresso + Compose UI testing
- Required for: Navigation flows, D-pad interaction tests

```bash
./gradlew connectedDebugAndroidTest
```

### Manual Test Checklist (required before PR)

- [ ] All screens navigable via D-pad only
- [ ] Player loads a stream end-to-end
- [ ] Player falls back to VLC when ExoPlayer fails
- [ ] App does not crash on a 1 GB RAM emulator after 10 minutes of playback
- [ ] ProGuard release build starts without `ClassNotFoundException`

---

## Pull Request Process

1. Branch from `develop`: `git checkout -b feature/your-feature develop`
2. Keep commits focused and atomic. Use conventional commit format:
   ```
   feat(player): add subtitle offset slider
   fix(home): restore focus after back navigation
   perf(image): cap Coil cache to 50 MB on low-end
   ```
3. Fill in the PR template fully.
4. CI must pass (lint + tests + build) before review.
5. At least **one maintainer approval** is required to merge.
6. Squash-merge into `develop`.

### Branch Naming

| Type | Pattern | Example |
|---|---|---|
| Feature | `feature/<short-desc>` | `feature/skip-timestamps` |
| Bug fix | `fix/<short-desc>` | `fix/vlc-crash-on-rotate` |
| Performance | `perf/<short-desc>` | `perf/reduce-buffer-size` |
| Docs | `docs/<short-desc>` | `docs/update-contributing` |
| CI/CD | `ci/<short-desc>` | `ci/add-nightly-build` |

---

## Release Process

Releases are managed by maintainers:

1. Merge `develop` → `main` via PR.
2. Tag `main` with a semantic version:
   ```bash
   git tag v1.2.0
   git push origin v1.2.0
   ```
3. The `release.yml` workflow builds signed per-ABI APKs and publishes a GitHub Release automatically.

### Versioning

We follow [Semantic Versioning](https://semver.org/):

- `MAJOR` — breaking changes (new minimum SDK, removed features)
- `MINOR` — new features, backwards-compatible
- `PATCH` — bug fixes

Pre-release identifiers: `-alpha.1`, `-beta.1`, `-rc.1`

---

## Questions?

Open a [Discussion](../../discussions) or reach out in the issue tracker. We're happy to help new contributors get oriented.
