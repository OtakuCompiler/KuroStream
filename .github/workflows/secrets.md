# GitHub Secrets Configuration for KuroStream CI/CD

This document lists all required GitHub Secrets that must be configured in the repository settings for the CI/CD workflows to function correctly.

## Required Secrets

### Android Signing (Release Builds)
| Secret Name | Description | Required For |
|-------------|-------------|--------------|
| `KEYSTORE_PATH` | Path to the release keystore file (relative to project root) | ci.yml - sign-and-release job |
| `KEYSTORE_PASSWORD` | Keystore password | ci.yml - sign-and-release job |
| `KEY_ALIAS` | Key alias in the keystore | ci.yml - sign-and-release job |
| `KEY_PASSWORD` | Key password (can be same as keystore password) | ci.yml - sign-and-release job |
| `SIGNING_KEY_BASE64` | Base64-encoded keystore file (alternative to file path) | ci.yml - sign-and-release job |

### Firebase App Distribution
| Secret Name | Description | Required For |
|-------------|-------------|--------------|
| `FIREBASE_APP_ID` | Firebase App ID (format: `1:123456789:android:abcdef123456`) | ci.yml, deploy-preview.yml |
| `FIREBASE_SERVICE_ACCOUNT` | JSON content of Firebase Service Account with `firebaseappdistro.admin` role | ci.yml, deploy-preview.yml |

### Notifications (Optional)
| Secret Name | Description | Required For |
|-------------|-------------|--------------|
| `SLACK_WEBHOOK_URL` | Slack Incoming Webhook URL for build notifications | ci.yml, nightly.yml, deploy-preview.yml |

### Google Play Console (Future Use)
| Secret Name | Description | Required For |
|-------------|-------------|--------------|
| `PLAY_CONSOLE_SERVICE_ACCOUNT` | Service account JSON for Google Play Console API | Not currently used |
| `PLAY_CONSOLE_PACKAGE_NAME` | Package name for Play Console (com.kurostream.app) | Not currently used |

## Setting Up Secrets

### 1. Generate Keystore
```bash
keytool -genkeypair -v \
  -keystore release.keystore \
  -alias kurostream \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass <KEYSTORE_PASSWORD> \
  -keypass <KEY_PASSWORD>
```

### 2. Base64 Encode Keystore (for SIGNING_KEY_BASE64)
```bash
base64 -i release.keystore -o keystore.base64
# Copy contents of keystore.base64 to SIGNING_KEY_BASE64 secret
```

### 3. Create Firebase Service Account
1. Go to Firebase Console → Project Settings → Service Accounts
2. Click "Generate new private key"
2. Copy the entire JSON content to `FIREBASE_SERVICE_ACCOUNT` secret
3. Ensure the service account has `Firebase App Distribution Admin` role

### 4. Get Firebase App ID
1. Go to Firebase Console → Project Settings → General
2. Copy the "App ID" for your Android app (format: `1:123456789:android:abcdef123456`)
2. Add to `FIREBASE_APP_ID` secret

### 5. Configure Slack Webhook (Optional)
1. In Slack, go to Apps → Incoming Webhooks → Add to Workspace
2. Choose channel (e.g., `#builds`, `#releases`, `#preview-deployments`)
3. Copy Webhook URL to `SLACK_WEBHOOK_URL` secret

## Workflow Secret Usage Matrix

| Workflow | Keystore | Firebase | Slack |
|----------|----------|----------|-------|
| ci.yml | ✅ (release tags) | ✅ (release tags) | ✅ |
| code-quality.yml | ❌ | ❌ | ❌ |
| nightly.yml | ❌ | ❌ | ✅ |
| deploy-preview.yml | ❌ | ✅ (develop branch) | ✅ |

## Security Best Practices

1. **Rotate secrets periodically** - Especially keystore passwords and service account keys
2. **Use least privilege** - Firebase service account should only have App Distribution permissions
3. **Never commit secrets** - All secrets must be configured in GitHub Settings → Secrets and variables → Actions
4. **Use environments** - Consider using GitHub Environments for production secrets with required reviewers
5. **Audit access** - Regularly review who has access to repository secrets

## Troubleshooting

### "Keystore not found" error
- Ensure `KEYSTORE_PATH` is correct relative to project root
- Or use `SIGNING_KEY_BASE64` instead

### "Firebase App Distribution upload failed"
- Verify `FIREBASE_SERVICE_ACCOUNT` has correct permissions
- Check `FIREBASE_APP_ID` matches the app in Firebase Console
- Ensure the app exists in Firebase project

### "Slack notification not sent"
- Verify `SLACK_WEBHOOK_URL` is valid
- Check Slack channel permissions
- Webhook URL must be for the correct workspace