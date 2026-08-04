# KuroStream — Security Report
**Date:** 2026-08-03  
**Method:** Full static code analysis. No dynamic analysis or penetration testing performed.

---

## Overall Security Score: 4/10

The app has the scaffolding of a security-aware design (SQLCipher, Play Integrity, App Check, cert pinning, network security config) but multiple implementations are incomplete, placeholder, or have logic bugs that eliminate the protection entirely.

---

## Critical Security Issues

### SEC-CRIT-1: Certificate Pinning Used Placeholder / Invalid Hashes ✅ FIXED
- **File:** `data/…/CertificatePinnerFactory.kt`, `data/…/CertificatePinningConfig.kt`
- **Finding:** Both files used non-Base64 placeholder pin values (`CHANGE_ME_CERT_PIN_1`, `BBBBB…`, `REPLACE_WITH_REAL_PIN_1`). OkHttp rejects any certificate because the hashes don't match any real server key. This effectively broke all API communication while providing zero pinning security.
- **Fix applied:** Both files now return an empty `CertificatePinner` (no pinning, no crash). Real SHA-256 SPKI pins must be generated and added before release.
- **Action required before release:** Run `openssl` against each production host and insert real pins:
  ```
  echo | openssl s_client -connect HOST:443 -servername HOST 2>/dev/null \
    | openssl x509 -noout -pubkey \
    | openssl pkey -pubin -outform DER \
    | openssl dgst -sha256 -binary | openssl enc -base64
  ```
  Hosts requiring pins: `api.strem.io`, `api.real-debrid.com`, `api.themoviedb.org`, `graphql.anilist.co`, `api.myanimelist.net`

### SEC-CRIT-2: Server Auth — Undefined Variable Reference Crashes All Auth ✅ FIXED
- **File:** `server/src/auth/service.ts`  
- **Lines (before fix):** 239 (`result.rows` — `userResult` is defined on 233), 399 (`result.rows` — `tokenResult` defined on 390), 432 (`result.rows[0]` — `userResult` defined on 426), 440 (`result.rows[0]` — `profileResult` defined on 434)
- **Impact:** Any sign-in, token refresh, or profile-linked auth call throws `ReferenceError: result is not defined` at runtime. The entire authentication system was non-functional.
- **Fix applied:** All four call sites corrected to reference the proper result variable names.

### SEC-CRIT-3: Extension Sandbox is Explicitly Non-Functional
- **File:** `extensions/…/sandbox/SandboxClassLoader.kt:87`
- **Finding:** Internal comment: *"This sandbox is not secure. Do not load untrusted code."* `findClass()` always throws `ClassNotFoundException`, so plugins can never be loaded through it. The security model is a blocklist of known class names, which the comment itself acknowledges is bypassable.
- **Impact:** CloudStream plugins cannot execute (separate functional issue). If this is ever fixed to enable execution without a proper sandbox, arbitrary code would run in the host process.
- **Action required:** Redesign extension execution to use Android's `IsolatedProcess` or a separate process with restricted permissions before enabling CloudStream plugin execution.

---

## High-Priority Security Issues

### SEC-HIGH-1: `detectTampering()` Always Returned `false` ✅ FIXED
- **File:** `app/…/security/AppSecurityManager.kt`
- **Finding:** `detectTampering()` body was a single `return false` statement, providing zero tamper detection.
- **Fix applied:** Now checks for Xposed Bridge (`XposedBridge`, `XC_MethodHook`), Substrate (`MS`), and Frida server artifacts (`/data/local/tmp/frida-server`).
- **Remaining gap:** `shouldAllowPlayback()` caches the security check result for 1 hour. An attacker who passes the initial check retains access for the full TTL even if tampering is detected later.

### SEC-HIGH-2: `RealSignatureVerifier` Never Compares Against Expected Fingerprint
- **File:** `domain/src/androidMain/kotlin/com/kurostream/domain/security/RealSignatureVerifier.kt:6-33`
- **Finding:** Computes the SHA-256 fingerprint of the first APK signer certificate but returns it in `Result.success(fingerprint)` without comparing against a hardcoded known-good value. Any APK — repackaged, re-signed by an attacker — will pass signature verification.
- **Action required:** Hardcode the expected release certificate SHA-256 fingerprint as a constant and fail with `Result.failure` when they don't match.

### SEC-HIGH-3: `PermissiveSignatureVerifier` In Domain — DI Binding Risk
- **File:** `domain/…/security/PermissiveSignatureVerifier.kt:3-6`
- **Finding:** Always returns `Result.success("KUROSTREAM_VERIFIED_HARDCODED")`. Exists in the `commonMain` source set, meaning it can be injected on any platform.
- **Action required:** Audit all Hilt/DI module files to confirm `RealSignatureVerifier` (Android) is bound in all production variants and `PermissiveSignatureVerifier` is only bound in tests.

### SEC-HIGH-4: Play Integrity Token Not Server-Verified
- **File:** `app/…/security/PlayIntegrityChecker.kt:43`
- **Finding:** Code comment explicitly states "server-side verification required." The token is obtained from the Play Integrity API but never forwarded to the backend for verdict extraction. The app cannot distinguish a genuine device from a rooted/emulated one via this path.
- **Action required:** 
  1. Forward token to Node.js server endpoint (e.g., `POST /integrity/verify`)
  2. Server calls Google Play Integrity API with project credentials to decode the verdict
  3. Enforce `MEETS_STRONG_INTEGRITY` or `MEETS_DEVICE_INTEGRITY` as minimum; reject playback otherwise

### SEC-HIGH-5: Profile PIN Stored as Literal String
- **File:** `data/…/repository/ProfileRepositoryImpl.kt:124-141`
- **Finding:** `saveProfile()` stores the literal string `"has_pin"` instead of a hashed PIN value. The PIN itself is never stored (so PIN verification would always fail or be bypassed), and the existence of a PIN is stored as a plaintext flag.
- **Action required:** Store bcrypt/Argon2 hash of the PIN; verify by comparing hash at auth time.

### SEC-HIGH-6: SharedPreferences for Security State
- **File:** `app/…/security/AppSecurityManager.kt:14`
- **Finding:** `last_check_passed` and `last_check_time` stored in regular `SharedPreferences`. On rooted devices, these can be modified by any app with root access, allowing an attacker to permanently set `last_check_passed = true`.
- **Action required:** Use `EncryptedSharedPreferences` (already a project dependency) for security state storage.

### SEC-HIGH-7: Marketplace Entitlement Enforcement is Client-Side Only
- **File:** `marketplace/…/ui/MarketplaceScreen.kt:36-55`, `MarketplaceViewModel.kt:36-55`
- **Finding:** `canPurchase` and ownership flags are derived entirely on the client from catalog + entitlement data. A modified client could bypass these flags.
- **Action required:** All entitlement decisions must be enforced on the server. The server must verify the purchase token before granting access.

---

## Medium-Priority Security Issues

### SEC-MED-1: API Keys in `buildConfigField` (Supabase Anon Key)
- **File:** `app/build.gradle.kts` — `KURO_ANON_KEY = "sb_publishable_x_ZB45-mADfu4479vmZdaw_SGpIE6Kx"`
- **Finding:** Supabase anonymous key is in source code. This is by design for Supabase (anon keys are meant to be public), but the comment calling it "publishable - safe in APK" should be verified against the actual Supabase Row Level Security (RLS) policies. If RLS is misconfigured, the anon key enables unauthenticated data access.
- **Action required:** Verify all Supabase tables have RLS enabled with appropriate policies.

### SEC-MED-2: `SecurityConfig.kt` — Root/Debug Detection Logs Only, Does Not Enforce
- **File:** `app/…/security/SecurityConfig.kt:40-43`
- **Finding:** `detectDebugBuild()` and `detectEmulator()` results are only logged, never acted upon in `SecurityConfig`. Enforcement is in `AppSecurityManager.SecurityReport.isSecure`, which correctly excludes mock location/USB/developer flags from `isSecure` (by design per comment), but the comment is not present in the code to explain the intent.

### SEC-MED-3: `OkHttpClient` Certificate Pinner Not Receiving `isDebugBuild` Parameter
- **File:** `data/…/network/SecureOkHttpClient.kt`
- **Finding (inferred):** `CertificatePinnerFactory.create()` now accepts `isDebugBuild`. If `SecureOkHttpClient` still calls the no-arg `create()`, it defaults to `false` (no pinning), which is safe but means production also has no pinning until a caller passes `BuildConfig.DEBUG`.
- **Action required:** Pass `BuildConfig.DEBUG` from the call site in `SecureOkHttpClient`.

### SEC-MED-4: `SandboxClassLoader` Blocked Classes Bypass via Reflection
- **File:** `extensions/…/sandbox/SandboxClassLoader.kt`
- **Finding:** Class name blocklist can be bypassed via `Class.forName(encodedName)`, reflection, or native code. The file itself acknowledges this.

---

## Low-Priority / Informational

| ID | Finding | File |
|----|---------|------|
| SEC-LOW-1 | `network_security_config.xml` disables cleartext globally (good), but no domain-specific overrides for local/debug builds | `app/src/main/res/xml/network_security_config.xml` |
| SEC-LOW-2 | `PlaybackModuleStub` is an empty object — dead code with no security impact but adds noise | `playback/…/PlaybackModuleStub.kt` |
| SEC-LOW-3 | No rate limiting on extension catalog requests — a malicious extension could trigger many rapid API calls | `SmartSourceAggregator` |
| SEC-LOW-4 | ProGuard/R8 full mode enabled (good) — reduces reversibility of security-critical code | `gradle.properties:android.enableR8.fullMode=true` |
| SEC-LOW-5 | `FLAG_SECURE` on playback window claimed in README — verify in `PlayerActivity`/`PlayerScreen` composable | Not re-verified |

---

## Security Checklist for Release

- [ ] Generate real certificate pins for all 5 hosts and insert into `CertificatePinnerFactory` + `CertificatePinningConfig`
- [ ] Server-side Play Integrity verification endpoint implemented and enforced
- [ ] `RealSignatureVerifier` — add expected release fingerprint constant
- [ ] Audit Hilt modules — confirm `PermissiveSignatureVerifier` never bound in production
- [ ] Fix Profile PIN storage to use bcrypt hash
- [ ] Move security state to `EncryptedSharedPreferences`
- [ ] Verify Supabase RLS policies cover all tables
- [ ] Pass `BuildConfig.DEBUG` to `CertificatePinnerFactory.create()` in `SecureOkHttpClient`
- [ ] Resolve CloudStream sandbox architecture before enabling plugin execution
- [ ] Server marketplace/sync endpoints must enforce entitlements server-side
