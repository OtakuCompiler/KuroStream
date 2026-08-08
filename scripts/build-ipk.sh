#!/usr/bin/env bash
# ============================================================================
# KuroStream LG webOS .ipk builder
# ============================================================================
# Produces an installable .ipk package for LG Smart TVs (webOS 4.0+).
#
# Usage:
#   ./scripts/build-ipk.sh                  # release build
#   ./scripts/build-ipk.sh --debug          # debug build
#   ./scripts/build-ipk.sh --version 1.2.0  # custom version
#
# Output:
#   build/KuroStream_1.0.0.ipk
#
# Install on TV (with developer mode enabled):
#   ares-install --device <DEVICE> build/KuroStream_1.0.0.ipk
#
# Or via the webOS CLI:
#   ares-package ./smarttv/webos
# ============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT_DIR"

BUILD_TYPE="release"
VERSION="1.0.0"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --debug) BUILD_TYPE="debug"; shift ;;
    --version) VERSION="$2"; shift 2 ;;
    --help|-h)
      sed -n '2,25p' "$0"
      exit 0 ;;
    *) echo "Unknown arg: $1"; exit 1 ;;
  esac
done

echo "==> Building KuroStream webOS .ipk ($BUILD_TYPE, v$VERSION)"

WEBAPP_SRC="$ROOT_DIR/smarttv/webapp"
WEBOS_DIR="$ROOT_DIR/smarttv/webos"
STAGING="$ROOT_DIR/build/webos-staging"
OUTPUT_DIR="$ROOT_DIR/build"
IPK_NAME="KuroStream_${VERSION}.ipk"

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

# 2. Update appinfo.json with version + build type
echo "    Configuring appinfo.json..."
APPINFO="$STAGING/appinfo.json"
if [[ ! -f "$APPINFO" ]]; then
  cp "$WEBOS_DIR/appinfo.json" "$APPINFO"
fi

python3 -c "
import json, sys
with open('$APPINFO') as f:
    info = json.load(f)
info['version'] = '$VERSION'
info['iconColor'] = '#E94560'
info['splashBackground'] = '#121212'
with open('$APPINFO', 'w') as f:
    json.dump(info, f, indent=2)
"

# 3. Build the .ipk (webOS ares-package is preferred; fallback to manual zip)
mkdir -p "$OUTPUT_DIR"

if command -v ares-package >/dev/null 2>&1; then
  echo "    Using ares-package..."
  ares-package "$STAGING" -o "$OUTPUT_DIR" 2>&1 | sed 's/^/      /'
  # Rename to our convention
  [[ -f "$OUTPUT_DIR/com.kurostream.app_$VERSION.ipk" ]] && \
    mv "$OUTPUT_DIR/com.kurostream.app_$VERSION.ipk" "$OUTPUT_DIR/$IPK_NAME" || true
else
  echo "    ares-package not installed; building ipk manually..."
  cd "$STAGING"
  zip -qr "$OUTPUT_DIR/$IPK_NAME" .
  cd "$ROOT_DIR"
fi

# 4. Validate
if [[ ! -f "$OUTPUT_DIR/$IPK_NAME" ]]; then
  echo "!! Failed to produce $IPK_NAME" >&2
  exit 1
fi

SIZE=$(du -h "$OUTPUT_DIR/$IPK_NAME" | cut -f1)
echo "==> Built $OUTPUT_DIR/$IPK_NAME ($SIZE)"

echo ""
echo "To install on webOS TV (developer mode):"
echo "  ares-install --device <TV_NAME> $OUTPUT_DIR/$IPK_NAME"
