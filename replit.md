# Kuro Stream Player

Android TV / Fire TV native anime streaming app with D-pad navigation, multi-provider support, and ExoPlayer + VLC fallback playback.

## Build & Run

```bash
# Debug build
./gradlew assembleDebug

# Unit tests
./gradlew testDebugUnitTest

# Release build (signing via env vars — see CI workflow)
./gradlew assembleRelease
```

## Stack

- **Language**: Kotlin 2.0, minSdk 24, targetSdk/compileSdk 35
- **UI**: Jetpack Compose + `androidx.tv.material3` (TV-optimised)
- **DI**: Hilt 2.51
- **Player**: ExoPlayer / Media3 1.3 (primary), VLC 3.6 (fallback)
- **Networking**: Retrofit 2.11 + OkHttp 4.12 + kotlinx-serialization
- **Storage**: Room 2.6 + DataStore 1.1
- **Image loading**: Coil 2.6
- **Build**: AGP 8.7, Gradle 8.9

## Where things live

| Path | Purpose |
|------|---------|
| `app/src/main/java/com/kurostream/tv/` | All Kotlin source |
| `app/src/main/java/.../ui/player/` | Player screen + ViewModel |
| `app/src/main/java/.../domain/provider/` | Provider abstraction + aggregator |
| `app/src/main/java/.../data/remote/skip/` | AniSkip timestamp service |
| `app/src/main/java/.../navigation/NavGraph.kt` | Compose navigation graph |
| `gradle/libs.versions.toml` | Version catalog (single source of truth) |
| `.github/workflows/` | CI/CD: android_build, ci, release, nightly, pr-check, dependency-review |

## Architecture decisions

- **Contract-first providers**: All streaming sources implement `AnimeProvider` interface; `ProviderAggregator` fans out requests in parallel and merges results.
- **ABI splits**: APKs are split by ABI (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64` + universal) to keep per-device download size well under 130 MB.
- **Memory-aware quality**: `MemoryAwareQualitySelector` caps quality at 720p on 1 GB devices, offers 4K on capable hardware.
- **Resource filtering**: Build restricts resource configs to `en`/`ja` to avoid shipping unused locale assets.
- **ExoPlayer-first, VLC fallback**: `SmartPlayerRouter` tries ExoPlayer for DIRECT/HLS/DASH; hands TORRENT and EXTERNAL streams to VLC.

## GitHub Workflow secrets (for release signing)

| Secret | Description |
|--------|-------------|
| `KEYSTORE_FILE` | Base64-encoded `.jks` keystore |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |

## Gotchas

- Run `./gradlew assembleDebug` from `kuro-stream/` — this is **not** part of the pnpm monorepo.
- `compileSdk = 35`; the `android_build.yml` CI workflow must install `platforms;android-35` and `build-tools;35.0.0`.
- VLC (`libvlc-all`) is the largest dependency (~40–50 MB per ABI); ABI splits ensure individual APKs stay under budget.
- `material-icons-extended` is large (~13 MB uncompressed) — remove from `compose` bundle in `libs.versions.toml` if icon usage is confirmed to come only from `android.R.drawable.*`.
