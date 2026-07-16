# Memory Regression Test Configuration

## GitHub Actions Workflow: memory-regression.yml

```yaml
name: Memory Regression Tests

on:
  push:
    branches: [main, develop]
    paths:
      - 'playback/src/main/java/com/kurostream/playback/memory/**'
      - 'playback/src/main/java/com/kurostream/playback/p2p/**'
      - 'benchmark/src/**'
  pull_request:
    branches: [main]
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM

jobs:
  memory-benchmark:
    name: Memory Benchmark
    runs-on: ubuntu-latest
    timeout-minutes: 60
    
    steps:
      - name: Checkout
        uses: actions/checkout@v4
        with:
          fetch-depth: 1

      - name: Setup JDK
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Setup Android SDK
        uses: android-actions/setup-android@v3
        with:
          api-level: 34
          ndk-version: r27c
          cmdline-tools-version: latest

      - name: Cache Gradle
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
          restore-keys: |
            ${{ runner.os }}-gradle-

      - name: Grant execute permission
        run: chmod +x ./gradlew

      - name: Build Benchmark Module
        run: ./gradlew :benchmark:assembleDebug --no-daemon -q

      - name: Run Memory Benchmark (Emulator)
        uses: android-actions/emulator-run@v3
        with:
          api-level: 34
          arch: arm64
          force-avd-creation: true
          emulator-options: -no-window -gpu swiftshader_indirect -memory 2048
          
      - name: Install Benchmark App
        run: |
          adb wait-for-device
          adb install -r benchmark/build/outputs/apk/debug/benchmark-debug.apk

      - name: Run Memory Benchmark
        run: |
          adb shell am start -n com.kurostream.benchmark/com.kurostream.benchmark.MemoryBenchmarkActivity
          # Wait for benchmark to complete
          sleep 300

      - name: Pull Results
        run: |
          adb pull /sdcard/kurostream/benchmark_results.json ./benchmark_results.json || true

      - name: Parse Results
        id: parse
        run: |
          if [ -f benchmark_results.json ]; then
            cat benchmark_results.json
            python3 -c "
import json, sys
with open('benchmark_results.json') as f:
    data = json.load(f)
for r in data['results']:
    peak = r['totalPssKb'] / 1024
    target = {'1080p_P2P_Direct': 30, '1080p_Upscale_4K': 40, '4K_Direct': 50, '4K_Atmos': 45, 'Idle_Home_Scrolling': 25}.get(r['scenario'], 100)
    status = 'PASS' if peak <= target else 'FAIL'
    print(f'{r[\"scenario\"]}: {peak:.1f}MB / {target}MB = {status}')
    if status == 'FAIL':
        sys.exit(1)
"
          else
            echo "No results file found"
            exit 1
          fi

      - name: Upload Benchmark Artifact
        uses: actions/upload-artifact@v4
        with:
          name: memory-benchmark-results
          path: benchmark_results.json
          retention-days: 30

      - name: Comment PR with Results
        if: github.event_name == 'pull_request'
        uses: actions/github-script@v7
        with:
          script: |
            const fs = require('fs');
            if (fs.existsSync('benchmark_results.json')) {
              const data = JSON.parse(fs.readFileSync('benchmark_results.json'));
              let body = '## 📊 Memory Benchmark Results\n\n';
              body += '| Scenario | Peak PSS | Avg PSS | Target | Status |\n';
              body += '|----------|----------|---------|--------|--------|\n';
              const targets = {'1080p_P2P_Direct': 30, '1080p_Upscale_4K': 40, '4K_Direct': 50, '4K_Atmos': 45, 'Idle_Home_Scrolling': 25};
              data.results.forEach(r => {
                const peak = (r.totalPssKb / 1024).toFixed(1);
                const avg = (r.pssKb / 1024).toFixed(1);
                const target = targets[r.scenario] || 100;
                const status = peak <= target ? '✅ PASS' : '❌ FAIL';
                body += `| ${r.scenario} | ${peak}MB | ${avg}MB | ${target}MB | ${status} |\n`;
              });
              github.rest.issues.createComment({
                issue_number: context.issue.number,
                owner: context.repo.owner,
                repo: context.repo.repo,
                body
              });
            }
```

## Memory Thresholds (Regression Detection)

| Scenario | Max Peak PSS | Max Avg PSS | Max Java Heap | Max Native Heap |
|----------|-------------|-------------|---------------|-----------------|
| 1080p P2P Direct | 30 MB | 25 MB | 15 MB | 8 MB |
| 1080p → 4K Upscale | 40 MB | 33 MB | 18 MB | 10 MB |
| 4K Direct | 50 MB | 42 MB | 20 MB | 12 MB |
| 4K + Atmos | 45 MB | 38 MB | 22 MB | 12 MB |
| Idle / Home Scrolling | 25 MB | 20 MB | 12 MB | 6 MB |

## Regression Detection Logic

```python
# Automatic failure if any scenario exceeds threshold
# Warning if avg PSS increases > 10% from baseline
# Critical if peak PSS exceeds threshold
```

## Baseline Storage

- Store baseline in `benchmark/baseline.json`
- Update baseline only on `main` branch after review
- Compare PR results against `main` baseline

## Local Development

```bash
# Run memory benchmark locally
./gradlew :benchmark:assembleDebug
adb install benchmark/build/outputs/apk/debug/benchmark-debug.apk
adb shell am start -n com.kurostream.benchmark/com.kurostream.benchmark.MemoryBenchmarkActivity

# Pull results
adb pull /sdcard/kurostream/benchmark_results.json
cat benchmark_results.json | python3 -m json.tool
```