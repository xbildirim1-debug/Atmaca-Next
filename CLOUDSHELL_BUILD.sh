#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
TOOLS="$HOME/atmaca-build-tools"
SDK="$HOME/android-sdk"
GRADLE_VERSION="9.5.0"
GRADLE_HOME="$TOOLS/gradle-$GRADLE_VERSION"
CMDLINE_REV="15859902"
BUILD_TOOLS="36.0.0"
PLATFORM="android-37"
OUT="$HOME/AtmacaNext-V19.apk"

log(){ printf '\n[ATMACA] %s\n' "$*"; }
fail(){ printf '\n[ATMACA][HATA] %s\n' "$*" >&2; exit 1; }

log "V19 Cloud Shell build basliyor"
mkdir -p "$TOOLS" "$SDK/cmdline-tools"

# Basic host tools. Google Cloud Shell normally has these; install only when missing.
MISSING=()
for c in curl unzip java; do command -v "$c" >/dev/null 2>&1 || MISSING+=("$c"); done
if ((${#MISSING[@]})); then
  log "Eksik host araclari kuruluyor: ${MISSING[*]}"
  sudo apt-get update
  sudo apt-get install -y curl unzip openjdk-17-jdk
fi

# AGP 9.3 requires JDK 17. Prefer JDK17 if available.
if command -v update-java-alternatives >/dev/null 2>&1 && ls /usr/lib/jvm/java-1.17.0-* >/dev/null 2>&1; then
  JAVA17=$(dirname "$(dirname "$(readlink -f /usr/lib/jvm/java-1.17.0-*/bin/java | head -1)")") || true
  [ -n "${JAVA17:-}" ] && export JAVA_HOME="$JAVA17"
fi
java -version 2>&1 | head -2

# Gradle 9.5.0 (AGP 9.3 official minimum/default).
if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  log "Gradle $GRADLE_VERSION indiriliyor"
  curl -fL --retry 4 --retry-delay 3 \
    "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
    -o "$TOOLS/gradle.zip"
  rm -rf "$GRADLE_HOME"
  unzip -q "$TOOLS/gradle.zip" -d "$TOOLS"
  rm -f "$TOOLS/gradle.zip"
fi
export PATH="$GRADLE_HOME/bin:$PATH"
gradle --version | sed -n '1,12p'

# Android command-line tools.
SDKMANAGER="$SDK/cmdline-tools/latest/bin/sdkmanager"
if [ ! -x "$SDKMANAGER" ]; then
  log "Android command-line tools indiriliyor"
  TMP="$TOOLS/android-cmdline"
  rm -rf "$TMP" && mkdir -p "$TMP"
  curl -fL --retry 4 --retry-delay 3 \
    "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_REV}_latest.zip" \
    -o "$TOOLS/cmdline-tools.zip"
  unzip -q "$TOOLS/cmdline-tools.zip" -d "$TMP"
  rm -rf "$SDK/cmdline-tools/latest"
  mkdir -p "$SDK/cmdline-tools/latest"
  cp -a "$TMP/cmdline-tools/." "$SDK/cmdline-tools/latest/"
  rm -rf "$TMP" "$TOOLS/cmdline-tools.zip"
fi
export ANDROID_HOME="$SDK"
export ANDROID_SDK_ROOT="$SDK"
export PATH="$SDK/platform-tools:$SDK/build-tools/$BUILD_TOOLS:$SDK/cmdline-tools/latest/bin:$PATH"

log "Android SDK lisanslari kabul ediliyor"
yes | sdkmanager --licenses >/dev/null || true
log "Android SDK API 37 + Build Tools $BUILD_TOOLS kuruluyor"
sdkmanager "platform-tools" "platforms;$PLATFORM" "build-tools;$BUILD_TOOLS"

echo "sdk.dir=$SDK" > "$PROJECT_DIR/local.properties"

log "Proje kimligi kontrol ediliyor"
grep -q 'applicationId = "com.atmacanext.app"' "$PROJECT_DIR/app/build.gradle.kts" || fail "applicationId beklenen com.atmacanext.app degil"
grep -q 'versionCode = 19' "$PROJECT_DIR/app/build.gradle.kts" || fail "versionCode 19 bulunamadi"
grep -q 'android:packageNames="com.twitter.android"' "$PROJECT_DIR/app/src/main/res/xml/accessibility_service_config.xml" || fail "Accessibility X paket filtresi bulunamadi"

cd "$PROJECT_DIR"
log "Gradle clean + unit test"
gradle --no-daemon --stacktrace clean test
log "Android lint"
gradle --no-daemon --stacktrace lint
log "Debug APK derleniyor"
gradle --no-daemon --stacktrace :app:assembleDebug

APK=$(find "$PROJECT_DIR/app/build/outputs/apk/debug" -maxdepth 1 -type f -name '*.apk' | head -1 || true)
[ -n "$APK" ] && [ -s "$APK" ] || fail "APK cikisi bulunamadi"

log "APK imza dogrulamasi"
apksigner verify --verbose --print-certs "$APK"
cp -f "$APK" "$OUT"

log "APK hazir"
ls -lh "$OUT"
sha256sum "$OUT" | tee "$HOME/AtmacaNext-V19.sha256.txt"

if command -v apkanalyzer >/dev/null 2>&1; then
  log "APK kimligi"
  apkanalyzer manifest application-id "$OUT" || true
  apkanalyzer manifest version-code "$OUT" || true
  apkanalyzer manifest version-name "$OUT" || true
fi

printf '\n============================================================\n'
printf 'ATMACA NEXT V19 BUILD TAMAMLANDI\n'
printf 'APK: %s\n' "$OUT"
printf '============================================================\n'
printf 'Indirmek icin: cloudshell download %q\n' "$OUT"
