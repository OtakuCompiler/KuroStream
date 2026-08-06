# KuroStream Build Guide

## Environment

- **OS:** Android (Termux) + proot Ubuntu (aarch64) **or** GitHub Actions (ubuntu-latest x86_64)
- **JDK:** Temurin/OpenJDK 17 (CI auto-provisions via `actions/setup-java@v4`)
- **Gradle:** 9.6.1 (wrapper-managed)
- **AGP:** 8.7.0
- **Kotlin:** 2.0.21
- **NDK:** 28.0.13004108

---

## Local Build (Termux / proot aarch64)

### Prerequisites

```bash
# Termux packages
pkg install proot-distro
proot-distro install ubuntu
proot-distro login ubuntu

# Inside proot Ubuntu
apk add qemu-x86_64  # needed for AAPT2 x86_64 binary
```

### AAPT2 x86_64 wrapper (one-time setup)

Google's `aapt2` is an x86-64 ELF binary. On aarch64 Termux/proot it needs QEMU user emulation.

```bash
# 1. Build x86_64 sysroot (run once)
mkdir -p /opt/x86_64-sysroot
cd /tmp/x64
wget http://deb.debian.org/debian/pool/main/g/glibc/libc6_2.36-9+deb12u14_amd64.deb
wget http://deb.debian.org/debian/pool/main/g/gcc-12/libgcc-s1_12.2.0-14+deb12u1_amd64.deb
for d in *.deb; do ar x "$d" 2>/dev/null || bsdtar -xf "$d" 2>/dev/null; tar -xf data.tar* -C /opt/x86_64-sysroot; done
cd /opt/x86_64-sysroot/lib64 && rm -f ld-linux-x86-64.so.2 && ln -s ../lib/x86_64-linux-gnu/ld-linux-x86-64.so.2 ld-linux-x86-64.so.2

# 2. Create wrapper
mkdir -p /opt/aapt2-qemu
cp /root/.gradle/caches/9.6.1/transforms/9648b9560a90e28c3959bb8bc46b84e2/transformed/aapt2-8.7.0-12006047-linux/aapt2 /opt/aapt2-qemu/aapt2.x86_64
cat > /opt/aapt2-qemu/aapt2 << 'EOF'
#!/bin/sh
unset LD_PRELOAD
exec /usr/bin/qemu-x86_64 -L /opt/x86_64-sysroot /opt/aapt2-qemu/aapt2.x86_64 "$@"
EOF
chmod +x /opt/aapt2-qemu/aapt2
```

> **Note:** AAPT2 under QEMU is ~5-10× slower than native. Expect `processDebugResources` to take 4-8 minutes on a Snapdragon 680. Kotlin compilation is unaffected.

### Build commands

```bash
cd /storage/emulated/0/kurostream

# Kotlin compile only (fast — no aapt2 needed)
bash gradlew :app:compileDebugKotlin --no-daemon --max-workers=1

# Full resources + compile (uses AAPT2 wrapper)
bash gradlew :app:processDebugResources \
  -Pandroid.aapt2FromMavenOverride=/opt/aapt2-qemu/aapt2 \
  --no-daemon --max-workers=1

# Full debug APK
bash gradlew :app:assembleDebug \
  -Pandroid.aapt2FromMavenOverride=/opt/aapt2-qemu/aapt2 \
  --no-daemon --max-workers=1
```

### Output

- APK: `/root/.kurostream-build/app/outputs/apk/debug/app-debug.apk`
- Gradle cache: `/root/.kurostream-gradle/`

### Tuning for 6 GB RAM device

The project is tuned for Snapdragon 680 / 6 GB RAM:
- Gradle heap: `-Xmx1024m` (in `gradle.properties`)
- Kotlin daemon: `-Xmx768m`
- `org.gradle.workers.max=1` (serialize AAPT2 to avoid translation-layer contention)
- `android.aapt2daemonMode=outofprocess` (in-process daemons fail under proot parallelism)
- `org.gradle.vfs.watch=false` (FUSE inotify is expensive)

Do **not** raise heaps above ~2.5 GB or the device OOM-kills the build.

---

## CI Build (GitHub Actions)

CI runs on `ubuntu-latest` (x86_64) — no AAPT2 emulation needed.

```bash
# Trigger CI
git push origin main
```

Workflows:
- **CI** (`.github/workflows/ci-cd.yml`): `:app:assembleDebug` + detekt
- **Code Quality** (`.github/workflows/code-quality.yml`): detekt static analysis
- **Release** (`.github/workflows/release.yml`): signed release APK/AAB

---

## Troubleshooting

### `AAPT2 Daemon startup failed` (x86-64 Exec format error)
You're on aarch64 without the qemu wrapper. Follow the AAPT2 x86_64 wrapper steps above.

### `JAVA_COMPILER capability missing`
Fixed in `build.gradle.kts` root `subprojects {}` block. Re-sync Gradle if the error persists:
```bash
bash gradlew --stop
bash gradlew :app:compileDebugKotlin --no-daemon
```

### `Duplicate class com.google.protobuf.AbstractMessageLite`
Fixed via `configurations.all` hardening in `app/build.gradle.kts`. Clean if the error persists:
```bash
bash gradlew clean
```

### OOM during build
Reduce workers to 1 and verify heaps in `gradle.properties`:
```bash
bash gradlew :app:assembleDebug --no-daemon --max-workers=1
```

### KSP/sqlite-jdbc "No matching toolchain"
Fixed via `resolutionStrategy.force("org.xerial:sqlite-jdbc:3.49.1.0")` in root `build.gradle.kts`.
