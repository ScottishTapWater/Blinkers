# Blinkers

<img src="res/drawable-nodpi/ic_launcher_art.png" alt="Blinkers icon" width="96" align="right">

[![CI](https://img.shields.io/github/actions/workflow/status/ScottishTapWater/Blinkers/ci.yml?branch=main&label=CI&style=for-the-badge)](https://github.com/ScottishTapWater/Blinkers/actions/workflows/ci.yml)
[![CD](https://img.shields.io/github/actions/workflow/status/ScottishTapWater/Blinkers/cd.yml?branch=main&label=CD&style=for-the-badge)](https://github.com/ScottishTapWater/Blinkers/actions/workflows/cd.yml)
[![Release](https://img.shields.io/github/v/release/ScottishTapWater/Blinkers?style=for-the-badge)](https://github.com/ScottishTapWater/Blinkers/releases/latest)

Blinkers activates the Galaxy S26 Ultra's hardware Privacy Display when you open the notification drawer or Quick Settings. Closing the panel removes Blinkers' privacy region.

Samsung supports automatic privacy for notification pop-ups, but does not offer the same setting for the expanded drawer. Blinkers adds that behaviour without root or Shizuku.

## Install

1. Download `Blinkers-VERSION.apk` from the [latest release](https://github.com/ScottishTapWater/Blinkers/releases/latest).
2. Install the APK and open Blinkers.
3. Open **Accessibility settings**, select **Installed apps**, and enable **Blinkers**.
4. Return to Blinkers and select **Enable protection**.
5. Open the notification drawer and check the effect from an angle.

Select **Disable protection** to turn Blinkers off. You can also disable its accessibility service in Android settings.

The earlier test app uses a different package ID. Disable its accessibility service when you switch to Blinkers.

## Compatibility and limits

Blinkers has been physically tested on a Galaxy S26 Ultra, model SM-S948B, running Android 17 and One UI 9. The APK requires Android 16 or later, but compatibility with other firmware has not been verified.

Blinkers protects the whole screen while the unlocked notification drawer or Quick Settings is open. Lock-screen activation is not implemented. Activation is event-driven, so there may be a brief exposure at the start of the opening swipe. Protection before the first visible notification frame has not been established.

The implementation uses undocumented Samsung framework methods. Firmware updates may change those methods or the System UI view identifiers used to detect the drawer. Blinkers is independent of Samsung.

## Accessibility and privacy

Blinkers uses an accessibility service to identify the drawer through window metadata and the root view's package and resource ID. Android requires window-content access for these APIs, but Blinkers does not read notification text or traverse notification content.

A transparent, non-touchable accessibility overlay stays ready while protection is enabled. Blinkers toggles the hardware privacy region on that overlay, so each opening does not have to wait for a new window to be laid out.

The app has no network permission, analytics, account system, or third-party runtime dependencies. It stores whether automatic protection is enabled.

## Build locally

Install JDK 17, Android SDK Platform 36, Android Build Tools 36.0.0, Bash, and Zip. Set the SDK paths, then build:

```sh
export ANDROID_JAR="$ANDROID_HOME/platforms/android-36/android.jar"
export ANDROID_BUILD_TOOLS="$ANDROID_HOME/build-tools/36.0.0"
bash tests/version-code.sh
bash build.sh
```

The debug APK is `build/blinkers.apk`. Debug builds use a local debug key and do not update an installed release build.

To build a release with the project's signing key:

```sh
export BUILD_TYPE=release
export VERSION=0.1.0
export APK_KEYSTORE=/path/to/release.p12
export APK_KEY_ALIAS=blinkers
export APK_STORE_PASSWORD
export APK_KEY_PASSWORD
bash build.sh
```

Set the two password variables in your environment before the build. Release builds require the signing certificate recorded in `signing-certificate.sha256`.

## Releases

CI builds a debug APK, tests version-code generation, checks shell scripts, and verifies the APK's metadata, alignment, and signature. Hardware privacy still requires a physical device check.

CD runs on pushes to `main`. It calculates the next version from conventional commits since the latest version tag, builds a signed release APK, and attaches the APK and its SHA-256 checksum to a GitHub release.

| Commit | Version change |
| --- | --- |
| `fix: correct drawer detection` | Patch |
| `perf: reduce activation delay` | Patch |
| `feat: add a setting` | Minor |
| `feat!: change supported behaviour` or a `BREAKING CHANGE:` footer | Major |
| `docs: update instructions` or `chore: update tooling` | No release |

Commit scopes are optional. The `v0.0.0` tag marks the initial baseline. The first feature commit produces `v0.1.0`. A manual CD run with no unreleased version change does not publish another release.

Android's numeric version code is `major * 1000000 + minor * 1000 + patch`. Minor and patch components must be below 1000, and the result must not exceed 2100000000. Development version `0.0.0-dev` uses version code 1.

The repository's Actions secrets contain `ANDROID_KEYSTORE_BASE64` and `ANDROID_KEYSTORE_PASSWORD`. The release job restores the key into the runner's temporary directory and removes it after signing. Pull-request builds use only a debug key.

Keep a separate backup of the release key and its password. Future APK updates require the same signing key. The public certificate fingerprint is in `signing-certificate.sha256`.

To verify a downloaded release:

```sh
sha256sum -c Blinkers-VERSION.apk.sha256
apksigner verify --print-certs Blinkers-VERSION.apk
```

Compare the certificate SHA-256 digest with `signing-certificate.sha256` in this repository.
