#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_JAR:?Set ANDROID_JAR to the SDK android.jar}"
: "${ANDROID_BUILD_TOOLS:?Set ANDROID_BUILD_TOOLS to the SDK build-tools directory}"
BUILD_TYPE="${BUILD_TYPE:-debug}"
VERSION="${VERSION:-0.0.0-dev}"
VERSION_CODE="$(bash scripts/version-code.sh "$VERSION")"
link_options=()
case "$BUILD_TYPE" in
    debug)
        APK_KEYSTORE=build/debug.keystore
        APK_KEY_ALIAS=androiddebugkey
        export APK_STORE_PASSWORD=android APK_KEY_PASSWORD=android
        link_options+=(--debug-mode)
        ;;
    release)
        : "${APK_KEYSTORE:?Set APK_KEYSTORE to the release keystore}"
        : "${APK_STORE_PASSWORD:?Set APK_STORE_PASSWORD}"
        : "${APK_KEY_PASSWORD:?Set APK_KEY_PASSWORD}"
        APK_KEY_ALIAS="${APK_KEY_ALIAS:-blinkers}"
        if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
            echo "Release builds require a stable VERSION." >&2
            exit 1
        fi
        ;;
    *) echo "BUILD_TYPE must be debug or release." >&2; exit 1 ;;
esac
rm -rf build/classes build/dex build/generated
mkdir -p build/classes build/dex
"$ANDROID_BUILD_TOOLS/aapt2" compile --dir res -o build/resources.zip
"$ANDROID_BUILD_TOOLS/aapt2" link -o build/unsigned.apk -I "$ANDROID_JAR" \
    --manifest AndroidManifest.xml --java build/generated \
    --version-code "$VERSION_CODE" --version-name "$VERSION" \
    "${link_options[@]}" build/resources.zip
javac --release 17 -classpath "$ANDROID_JAR" -d build/classes \
    src/io/github/scottishtapwater/blinkers/*.java build/generated/io/github/scottishtapwater/blinkers/R.java
jar cf build/classes.jar -C build/classes .
"$ANDROID_BUILD_TOOLS/d8" --lib "$ANDROID_JAR" --min-api 36 --output build/dex build/classes.jar
(cd build/dex && zip -q -u ../unsigned.apk classes.dex)
"$ANDROID_BUILD_TOOLS/zipalign" -f 4 build/unsigned.apk build/aligned.apk
if [[ "$BUILD_TYPE" == debug && ! -f "$APK_KEYSTORE" ]]; then
    keytool -genkeypair -keystore "$APK_KEYSTORE" -storepass:env APK_STORE_PASSWORD \
        -keypass:env APK_KEY_PASSWORD -alias "$APK_KEY_ALIAS" -dname 'CN=Android Debug' \
        -keyalg RSA -keysize 2048 -validity 3650
fi
"$ANDROID_BUILD_TOOLS/apksigner" sign --ks "$APK_KEYSTORE" --ks-key-alias "$APK_KEY_ALIAS" \
    --ks-pass env:APK_STORE_PASSWORD --key-pass env:APK_KEY_PASSWORD --v4-signing-enabled false \
    --out build/blinkers.apk build/aligned.apk
bash scripts/check-apk.sh "$VERSION" "$BUILD_TYPE"
