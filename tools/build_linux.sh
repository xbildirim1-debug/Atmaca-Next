#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
: "${ANDROID_SDK_ROOT:=${ANDROID_HOME:-$HOME/android-sdk}}"
export ANDROID_SDK_ROOT ANDROID_HOME="$ANDROID_SDK_ROOT"
if ! command -v gradle >/dev/null 2>&1; then
  echo 'Gradle 9.5.0 required (AGP 9.3). Use GitHub Actions workflow if not installed locally.' >&2
  exit 20
fi
if ! command -v sdkmanager >/dev/null 2>&1; then
  echo 'Android sdkmanager required. Use GitHub Actions workflow if not installed locally.' >&2
  exit 21
fi
sdkmanager "platform-tools" "platforms;android-37" "build-tools;36.0.0"
gradle --no-daemon clean test lint assembleDebug --stacktrace
APK="app/build/outputs/apk/debug/app-debug.apk"
test -s "$APK"
BT="$(find "$ANDROID_SDK_ROOT/build-tools" -type f -name apksigner | sort -V | tail -1)"
"$BT" verify --verbose --print-certs "$APK"
mkdir -p dist
cp "$APK" dist/AtmacaNext-V19-debug.apk
sha256sum dist/AtmacaNext-V19-debug.apk > dist/AtmacaNext-V19-debug.apk.sha256
echo "APK: $ROOT/dist/AtmacaNext-V19-debug.apk"
