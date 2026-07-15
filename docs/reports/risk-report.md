# Risk Report

## High Risk

### 1. Large Binary in Repository
- **File:** `ktlint` (76.74 MB)
- **Impact:** Repository bloat, clone performance, GitHub warnings
- **Mitigation:** Added to .gitignore; consider Git LFS or remove from history with `git filter-repo`

### 2. Empty/Stub Files in :launcher Module
- **Details:** 18 of ~65 Kotlin files are empty (0 bytes), including all 4 Hilt DI modules and Firebase layer
- **Impact:** Module will not compile end-to-end
- **Reference:** MERGE_REPORT_2.md (in docs/)

### 3. Empty Resources in :launcher Module
- **Details:** All resource files (5 layouts, 2 values files, 1 preferences XML) and both ML assets (.tflite model, labels) are empty
- **Impact:** Runtime crashes if accessed

### 4. :sync Module Not Included in Build
- **Details:** Module exists at `sync/` with build.gradle.kts but not in settings.gradle.kts
- **Impact:** Dead code, confusion

### 5. Native Library Dependencies
- **Modules:** :playback (MPV, Oboe), :launcher (TensorFlow Lite)
- **Risk:** Require manual setup (CMake, prebuilt AARs), not on Maven Central
- **Impact:** Build failures on fresh environments

## Medium Risk

### 6. ExoPlayer Version Conflict (Resolved)
- **Previous:** :launcher used legacy ExoPlayer2 (`com.google.android.exoplayer2`)
- **Current:** Migrated to Media3 (`androidx.media3`) to align with :playback
- **Risk:** Potential behavioral differences

### 7. Detekt Version Pinning
- **Version:** 1.23.6 (pinned due to 1.24.0 ClassCastException bug)
- **Risk:** Missing new rules, security fixes

### 8. Missing Documentation Structure
- **Status:** 17 markdown files at root moved to docs/
- **Risk:** No organized documentation site

### 9. No CI/CD for WebOS/Tizen
- **Status:** Kotlin/JS modules (webosApp, tizenApp) not built in CI
- **Risk:** Regressions undetected

## Low Risk

### 10. Gradle Wrapper Binary in Repo
- **File:** `gradle-wrapper.jar` (small, standard)

### 11. Duplicate Directory Listings
- **Observation:** Some directories appeared multiple times in `ls` output due to command formatting
- **Status:** No actual duplicates found

### 12. Build Logic Convention Plugins
- **Status:** Well-structured in `build-logic/`
- **Risk:** Low, but custom plugins need maintenance

## Recommendations

1. **Remove ktlint binary** from git history using `git filter-repo` or BFG Repo-Cleaner
2. **Complete :launcher implementation** or mark as experimental/disabled
3. **Include :sync in settings.gradle.kts** or remove if obsolete
4. **Document native library setup** in README/CONTRIBUTING
5. **Add WebOS/Tizen to CI pipeline** (GitHub Actions)
6. **Update Detekt** when ClassCastException bug is fixed upstream
7. **Create documentation site** from docs/ (e.g., GitBook, MkDocs, Docusaurus)
8. **Add pre-commit hooks** for ktlint/spotless
9. **Enable GitHub features:** Dependabot, CodeQL, branch protection rules
10. **Add CODEOWNERS file** for review assignments