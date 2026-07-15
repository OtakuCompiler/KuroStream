# Module Dependency Graph

## Core Modules (Kotlin Multiplatform)
```
core-common (KMP: Android, JVM, JS)
  └── core-platform (KMP: Android, JVM, iOS, JS)
        └── domain (KMP: JVM, JS, Browser, Node)
              └── sync (KMP: JVM, Android, iOS)
```

## Android Library Modules
```
common (Android) ← core-common
data (Android) ← core-common, common, domain, cache
cache (Android) ← common, domain
plugin-sdk (Android) ← core-common, domain, common
playback (Android) ← domain
extensions (Android) ← domain, common
launcher (Android) ← domain, common
torrent (Android) ← core-common, domain, common, cache
backup (Android) ← core-common, domain, common, data
benchmark (Android) ← domain, data, playback
lint-checks (Android)
```

## Application
```
app (Android Application)
  ← core-common, common, domain, data, cache, plugin-sdk
  ← playback, extensions, launcher, backup
```

## Platform-Specific (KMP JS)
```
webosApp (KMP: JS) ← core-platform
tizenApp (KMP: JS)
```

## Build Logic
```
build-logic (Convention Plugins)
  ├── AndroidApplicationConventionPlugin
  ├── AndroidLibraryConventionPlugin
  ├── KotlinMultiplatformConventionPlugin
  ├── DetektConventionPlugin
  ├── HiltConventionPlugin
  └── SpotlessConventionPlugin
```

## Dependency Direction Rules
- ✅ app → all libraries
- ✅ domain → core-common, core-platform
- ✅ data → domain, core-common, common, cache
- ✅ All feature modules → domain, common
- ✅ core-platform → core-common
- ❌ No cycles allowed
- ❌ Lower layers must not depend on higher layers
