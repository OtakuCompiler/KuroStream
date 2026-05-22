#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# generate-keystore.sh
# Generates a release keystore for kuro-stream and prints the base64-encoded
# value you need to paste into GitHub Secrets → KEYSTORE_BASE64.
#
# Usage:
#   chmod +x scripts/generate-keystore.sh
#   ./scripts/generate-keystore.sh
# ─────────────────────────────────────────────────────────────────────────────

set -euo pipefail

KEYSTORE_FILE="app/keystore.jks"
KEY_ALIAS="kuro-stream-key"
VALIDITY_DAYS=10000

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  kuro-stream — Keystore Generator"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

if [ -f "$KEYSTORE_FILE" ]; then
  echo "⚠️  $KEYSTORE_FILE already exists."
  read -rp "   Overwrite? [y/N]: " CONFIRM
  if [[ "$CONFIRM" != "y" && "$CONFIRM" != "Y" ]]; then
    echo "Aborted."
    exit 0
  fi
  rm -f "$KEYSTORE_FILE"
fi

read -rsp "Enter keystore password (min 6 chars): " KEYSTORE_PASS
echo ""
read -rsp "Confirm keystore password: " KEYSTORE_PASS2
echo ""

if [ "$KEYSTORE_PASS" != "$KEYSTORE_PASS2" ]; then
  echo "❌  Passwords do not match. Aborting."
  exit 1
fi

read -rsp "Enter key password (leave blank to use same as keystore): " KEY_PASS
echo ""
KEY_PASS="${KEY_PASS:-$KEYSTORE_PASS}"

echo ""
echo "Generating keystore at: $KEYSTORE_FILE"
echo "Key alias:              $KEY_ALIAS"
echo "Validity:               $VALIDITY_DAYS days"
echo ""

keytool -genkeypair \
  -v \
  -keystore "$KEYSTORE_FILE" \
  -keyalg RSA \
  -keysize 2048 \
  -validity "$VALIDITY_DAYS" \
  -alias "$KEY_ALIAS" \
  -storepass "$KEYSTORE_PASS" \
  -keypass "$KEY_PASS" \
  -dname "CN=kuro-stream, OU=Mobile, O=kuro-stream, L=Unknown, S=Unknown, C=US"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  ✅  Keystore generated successfully!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

BASE64_VALUE=$(base64 < "$KEYSTORE_FILE" | tr -d '\n')

echo "────────────────────────────────────────────────────"
echo "  GitHub Secrets — copy these values exactly:"
echo "────────────────────────────────────────────────────"
echo ""
echo "Secret name:   KEYSTORE_BASE64"
echo "Secret value:"
echo ""
echo "$BASE64_VALUE"
echo ""
echo "────────────────────────────────────────────────────"
echo "Secret name:   KEYSTORE_PASSWORD"
echo "Secret value:  $KEYSTORE_PASS"
echo ""
echo "Secret name:   KEY_ALIAS"
echo "Secret value:  $KEY_ALIAS"
echo ""
echo "Secret name:   KEY_PASSWORD"
echo "Secret value:  $KEY_PASS"
echo "────────────────────────────────────────────────────"
echo ""
echo "⚠️  IMPORTANT:"
echo "   - Add 'app/keystore.jks' to your .gitignore immediately."
echo "   - Never commit keystore.jks or keystore.properties to git."
echo "   - Store these secret values somewhere safe (password manager)."
echo ""

# Offer to update .gitignore
if ! grep -q "keystore.jks" .gitignore 2>/dev/null; then
  echo "Adding keystore entries to .gitignore..."
  cat >> .gitignore <<'GITIGNORE'

# Android signing — never commit these
*.jks
*.keystore
keystore.properties
GITIGNORE
  echo "✅  .gitignore updated."
fi
