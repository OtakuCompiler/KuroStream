# GitHub Secrets Required for KuroStream CI/CD

This document lists all the GitHub Secrets that must be configured in the repository settings for the CI/CD workflows to function correctly.

## Required Secrets

### Android Signing (Release Builds)
| Secret Name | Description | Example |
|-------------|-------------|---------|
| `KEYSTORE_PASSWORD` | Password for the keystore file | `keystore_pass_123` |
| `KEY_ALIAS` | Key alias in the keystore | `kurostream_key` |
| `KEY_PASSWORD` | Password for the specific key | `key_pass_456` |
| `SIGNING_KEY_BASE64` | Base64-encoded keystore file | `base64 -i release.keystore \| pbcopy` |

### Firebase App Distribution
| Secret Name | Description | Source |
|-------------|-------------|--------|
| `FIREBASE_APP_ID` | Firebase App ID for production | Firebase Console → Project Settings → General → Your Apps |
| `FIREBASE_APP_ID_PREVIEW` | Firebase App ID for preview builds | Firebase Console → Project Settings → General → Your Apps |
| `FIREBASE_SERVICE_ACCOUNT` | Service account JSON (full content) | Firebase Console → Project Settings → Service Accounts → Generate New Private Key |

### Notifications
| Secret Name | Description | Source |
|-------------|-------------|--------|
| `SLACK_WEBHOOK_URL` | Incoming webhook URL for Slack notifications | Slack App → Incoming Webhooks |

## Optional Secrets

### Code Quality
| Secret Name | Description |
|-------------|-------------|
| `SONAR_TOKEN` | SonarCloud token for code quality analysis |
| `CODECOV_TOKEN` | Codecov token for coverage reporting |

### Dependency Management
| Secret Name | Description |
|-------------|-------------|
| `GITHUB_TOKEN` | Automatically provided by GitHub Actions (no need to configure) |
| `MAVEN_CENTRAL_USERNAME` | For publishing to Maven Central |
| `MAVEN_CENTRAL_PASSWORD` | For publishing to Maven Central |

## How to Generate Signing Key

```bash
# Generate keystore
keytool -genkey -v \
  -keystore release.keystore \
  -alias kurostream_key \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass YOUR_KEYSTORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD

# Encode to Base64
base64 -i release.keystore | pbcopy  # macOS
base64 -w 0 release.keystore | xclip -selection clipboard  # Linux
```

## Firebase Service Account Setup

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Go to Project Settings → Service Accounts
4. Click "Generate New Private Key"
5. Copy the entire JSON content to `FIREBASE_SERVICE_ACCOUNT` secret

## Adding Secrets to GitHub

1. Go to your repository on GitHub
2. Navigate to **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Add each secret with the exact name from the tables above

## Verification

After adding all secrets, run the workflow manually to verify:
1. Go to **Actions** tab
2. Select **CI** workflow
3. Click **Run workflow** → **Run workflow**

The build should complete successfully with all artifacts uploaded and Firebase distribution working.