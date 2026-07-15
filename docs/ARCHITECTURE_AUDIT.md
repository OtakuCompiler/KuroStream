# KuroStream Architecture Audit Report

## STEP 2: ARCHITECTURE VERIFICATION

### DEPENDENCY GRAPH ANALYSIS

```
ROOT MODULES (No dependencies):
- core-common (KMP: jvm, js, android)

CORE LAYER:
- domain (depends on: core-common, common)
  ❌ ARCHITECTURE VIOLATION: domain depends on common (UI/Android layer)

DATA LAYER:
- data (depends on: core-common, common, domain, cache)
  ❌ ARCHITECTURE VIOLATION: data depends on common (UI/Android layer)
- cache (depends on: common, domain)
  ❌ ARCHITECTURE VIOLATION: cache depends on common (UI/Android layer)

PRESENTATION/EXTENSION LAYER:
- common (depends on: core-common)
- playback (depends on: domain)
- plugin-sdk (depends on: core-common, common, domain)
  ❌ ARCHITECTURE VIOLATION: plugin-sdk depends on common (UI layer)
- launcher (depends on: common, domain)
- extensions (depends on: common, domain)
- backup (depends on: core-common, common, domain, data)
  ❌ ARCHITECTURE VIOLATION: backup depends on common (UI layer)
- torrent (depends on: cache, common, core-common, domain)
  ❌ ARCHITECTURE VIOLATION: torrent depends on common (UI layer)

APP LAYER:
- app (depends on: ALL modules)

KMP MODULES:
- sync (depends on: domain, common)
  ❌ ARCHITECTURE VIOLATION: sync depends on common (UI layer)
- webosApp (depends on: none - pure KMP)
- tizenApp (depends on: none - pure KMP)

TEST/TOOLING:
- benchmark (depends on: app, playback)
  ❌ CIRCULAR DEPENDENCY RISK: benchmark depends on app
- lint-checks (depends on: none)

```

### CRITICAL ARCHITECTURE VIOLATIONS FOUND:

1. **Domain Layer Contamination**
   - domain/build.gradle.kts:71-72 - domain depends on common module
   - domain/build.gradle.kts:105-106 - domain has jvmMain depending on common
   - This violates Clean Architecture: Domain should have ZERO dependencies on UI/Android

2. **Data Layer Contamination** 
   - data depends on common (UI layer)
   - cache depends on common (UI layer)
   - Data layer should only depend on domain, not presentation

3. **Plugin SDK Leakage**
   - plugin-sdk depends on common (UI layer)
   - Plugin SDK should be framework-agnostic

4. **Circular Dependency Risk**
   - benchmark depends on app (reverse dependency)
   - This creates potential circular dependencies

5. **KMP Module Issues**
   - sync module depends on common (Android-specific)
   - sync should be platform-agnostic or properly abstracted

### EXPECTED CLEAN ARCHITECTURE:
```
core-common (Pure Kotlin - No Android)
    ↓
domain (Pure Kotlin - No Android, No UI)
    ↓
data (Android - depends only on domain)
    ↓
common (Android UI - depends on domain/data)
    ↓
app (depends on all)
```

### IMMEDIATE FIXES REQUIRED: