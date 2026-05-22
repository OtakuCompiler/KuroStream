# GitHub Secrets Setup for kuro-stream

This document explains how to configure the required GitHub Secrets for
APK signing and the CI/CD workflows.

---

## Required Secrets

| Secret Name        | Description                                      |
|--------------------|--------------------------------------------------|
| `KEYSTORE_BASE64`  | Base64-encoded content of your `.jks` keystore   |
| `KEYSTORE_PASSWORD`| Password for the keystore file                   |
| `KEY_ALIAS`        | Alias of the signing key inside the keystore     |
| `KEY_PASSWORD`     | Password for the signing key                     |

---

## Step 1 — Generate a Keystore (one-time setup)

### Option A: Use the helper script (recommended)

```bash
chmod +x scripts/generate-keystore.sh
./scripts/generate-keystore.sh
```

The script generates `app/keystore.jks`, prints the base64 value, and lists all
four secret values for you to copy.

### Option B: Run keytool manually

```bash
keytool -genkeypair \
  -v \
  -keystore app/keystore.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias kuro-stream-key \
  -storepass YOUR_KEYSTORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD \
  -dname "CN=kuro-stream, OU=Mobile, O=YourOrg, L=City, S=State, C=US"
```

---

## Step 2 — Convert the Keystore to Base64

**macOS / Linux:**

```bash
base64 -i app/keystore.jks | tr -d '\n'
```

**Windows (PowerShell):**

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("app\keystore.jks"))
```

Copy the entire output — that is the value of `KEYSTORE_BASE64`.

---

## Step 3 — Add Secrets to GitHub

1. Open your repository on GitHub.
2. Go to **Settings → Secrets and variables → Actions**.
3. Click **New repository secret** for each of the four secrets:

```
KEYSTORE_BASE64     → (the long base64 string from Step 2)
KEYSTORE_PASSWORD   → (your keystore password)
KEY_ALIAS           → kuro-stream-key   (or your chosen alias)
KEY_PASSWORD        → (your key password)
```

---

## Step 4 — Protect the Keystore Locally

Add these lines to your `.gitignore` (the generator script does this automatically):

```gitignore
# Android signing — never commit these
*.jks
*.keystore
keystore.properties
```

**Never** commit `keystore.jks` or `keystore.properties` to git.

---

## Step 5 — Wire Up `build.gradle` (app module)

Add signing config to `app/build.gradle` so local release builds also work:

```groovy
// app/build.gradle

def keystorePropertiesFile = rootProject.file("keystore.properties")
def keystoreProperties = new Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(new FileInputStream(keystorePropertiesFile))
}

android {
    ...

    signingConfigs {
        release {
            // CI injects via -P flags; local uses keystore.properties
            storeFile     file(keystoreProperties['storeFile'] ?: System.getenv('KEYSTORE_FILE') ?: 'keystore.jks')
            storePassword keystoreProperties['storePassword'] ?: System.getenv('KEYSTORE_PASSWORD')
            keyAlias      keystoreProperties['keyAlias']      ?: System.getenv('KEY_ALIAS')
            keyPassword   keystoreProperties['keyPassword']   ?: System.getenv('KEY_PASSWORD')
        }
    }

    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

For local release builds, copy `keystore.properties.example` → `keystore.properties`
and fill in your values.

---

## Triggering a Release

Push a tag in `vX.Y.Z` format to trigger the release workflow:

```bash
git tag v1.0.0
git push origin v1.0.0
```

The workflow will:
1. Run lint + tests
2. Build the debug APK
3. Build the signed release APK
4. Package everything into a ZIP
5. Create a GitHub Release with all files attached
