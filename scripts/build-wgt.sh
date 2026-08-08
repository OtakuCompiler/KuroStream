#!/usr/bin/env bash
# ============================================================================
# KuroStream Samsung Tizen .wgt builder
# ============================================================================
# Produces an installable .wgt package for Samsung Tizen TVs (4.0+).
#
# Usage:
#   ./scripts/build-wgt.sh                  # release build
#   ./scripts/build-wgt.sh --version 1.2.0  # custom version
#
# Output:
#   build/KuroStream_1.0.0.wgt
#
# Install on TV (with developer mode enabled):
#   tizen install --pkg build/KuroStream_1.0.0.wgt -s <DEVICE>
# ============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT_DIR"

VERSION="1.0.0"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --version) VERSION="$2"; shift 2 ;;
    --help|-h)
      sed -n '2,22p' "$0"
      exit 0 ;;
    *) echo "Unknown arg: $1"; exit 1 ;;
  esac
done

echo "==> Building KuroStream Tizen .wgt (v$VERSION)"

WEBAPP_SRC="$ROOT_DIR/smarttv/webapp"
TIZEN_DIR="$ROOT_DIR/smarttv/tizen"
STAGING="$ROOT_DIR/build/tizen-staging"
OUTPUT_DIR="$ROOT_DIR/build"
WGT_NAME="KuroStream_${VERSION}.wgt"

# 1. Stage webapp assets
echo "    Staging webapp assets..."
rm -rf "$STAGING"
mkdir -p "$STAGING/assets" "$STAGING/shaders" "$STAGING/ffmpeg-wasm"

cp -r "$WEBAPP_SRC/index.html" "$STAGING/"
cp -r "$WEBAPP_SRC/styles.css" "$STAGING/"
cp -r "$WEBAPP_SRC/icon-128.png" "$STAGING/"
cp -r "$WEBAPP_SRC/icon-256.png" "$STAGING/"
cp -r "$WEBAPP_SRC/splash.png" "$STAGING/"
cp -r "$WEBAPP_SRC/dist/"* "$STAGING/" 2>/dev/null || true
cp -r "$WEBAPP_SRC/assets/"* "$STAGING/assets/" 2>/dev/null || true
cp -r "$WEBAPP_SRC/src/"* "$STAGING/" 2>/dev/null || true
cp -r "$WEBAPP_SRC/shaders/"* "$STAGING/shaders/" 2>/dev/null || true
cp -r "$WEBAPP_SRC/ffmpeg-wasm/"* "$STAGING/ffmpeg-wasm/" 2>/dev/null || true

# 2. Update config.xml with version
echo "    Configuring config.xml..."
CONFIG="$STAGING/config.xml"
if [[ ! -f "$CONFIG" ]]; then
  cp "$TIZEN_DIR/config.xml" "$CONFIG"
fi

sed -i.bak "s/version=\"[^\"]*\"/version=\"$VERSION\"/" "$CONFIG"
rm -f "$CONFIG.bak"

# 3. Build the .wgt
mkdir -p "$OUTPUT_DIR"

if command -v tizen >/dev/null 2>&1; then
  echo "    Using tizen CLI..."
  cd "$STAGING"
  tizen package-web -s . -t wgt -o "$OUTPUT_DIR" 2>&1 | sed 's/^/      /'
  cd "$ROOT_DIR"
else
  echo "    tizen CLI not installed; building wgt manually..."
  cd "$STAGING"
  # Tizen .wgt is a zip file with config.xml at root
  zip -qr "$OUTPUT_DIR/$WGT_NAME" .
  cd "$ROOT_DIR"
fi

# 4. Sign the .wgt if author/cert is available
if [[ -f "$HOME/.tizen/author.p12" ]]; then
  echo "    Signing wgt with author certificate..."
  AUTHOR_PWD="${TIZEN_AUTHOR_PASSWORD:-tizen}"
  if command -v tizen >/dev/null 2>&1; then
    tizen sign --sign "$OUTPUT_DIR/$WGT_NAME" \
      --signer "$HOME/.tizen/author.p12" \
      --password "$AUTHOR_PWD" 2>&1 | sed 's/^/      /' || \
      echo "      (signing skipped — set TIZEN_AUTHOR_PASSWORD env var)"
  fi
fi

# 5. Validate
if [[ ! -f "$OUTPUT_DIR/$WGT_NAME" ]]; then
  echo "!! Failed to produce $WGT_NAME" >&2
  exit 1
fi

SIZE=$(du -h "$OUTPUT_DIR/$WGT_NAME" | cut -f1)
echo "==> Built $OUTPUT_DIR/$WGT_NAME ($SIZE)"

echo ""
echo "To install on Tizen TV (developer mode):"
echo "  tizen install --pkg $OUTPUT_DIR/$WGT_NAME -s <TV_NAME>"
