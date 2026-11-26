#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
version="${1:?Usage: check-apk.sh VERSION BUILD_TYPE}"
build_type="${2:?Usage: check-apk.sh VERSION BUILD_TYPE}"
: "${ANDROID_BUILD_TOOLS:?Set ANDROID_BUILD_TOOLS}"
metadata="$("$ANDROID_BUILD_TOOLS/aapt2" dump badging build/blinkers.apk)"
code="$(bash scripts/version-code.sh "$version")"
[[ "$metadata" == *"package: name='io.github.scottishtapwater.blinkers'"* ]]
[[ "$metadata" == *"versionCode='$code'"* ]]
[[ "$metadata" == *"versionName='$version'"* ]]
[[ "$metadata" == *"application-label:'Blinkers'"* ]]
[[ "$metadata" == *"sdkVersion:'36'"* || "$metadata" == *"minSdkVersion:'36'"* ]]
[[ "$metadata" == *"targetSdkVersion:'36'"* ]]
[[ "$metadata" == *"application-icon-"* ]]
[[ "$metadata" != *"uses-permission:"* ]]
if [[ "$build_type" == release ]]; then
    [[ "$metadata" != *"application-debuggable"* ]]
else
    [[ "$metadata" == *"application-debuggable"* ]]
fi
"$ANDROID_BUILD_TOOLS/zipalign" -c 4 build/blinkers.apk
certificate="$("$ANDROID_BUILD_TOOLS/apksigner" verify --verbose --print-certs build/blinkers.apk)"
if [[ "$build_type" == release ]]; then
    expected="$(cat signing-certificate.sha256)"
    [[ "$certificate" == *"Signer #1 certificate SHA-256 digest: $expected"* ]]
fi
printf 'APK verified: Blinkers %s, versionCode %s, %s signing.\n' "$version" "$code" "$build_type"
