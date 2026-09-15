# List available recipes
default:
    @just --list

# Build, install, and launch the debug app on the connected emulator/device
dev:
    ./gradlew :app:installDebug
    adb shell monkey -p com.spendr.app.kt 1

# Full validation: debug APK, lint, and JVM unit tests
check:
    ./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest

# Build the signed release APK into _apk/spendr-kt-<version>-<d.mmm-HH-MM>.apk and upload it
build-apk:
    #!/usr/bin/env bash
    set -euo pipefail
    ./gradlew :app:assembleRelease
    mkdir -p _apk
    ver="$(sed -n 's/.*versionName = "\(.*\)"/\1/p' app/build.gradle.kts | head -1)"
    stamp="$(date +"%-d.%b-%H-%M" | tr 'A-Z' 'a-z')"
    apk="_apk/spendr-kt-${ver}-${stamp}.apk"
    cp app/build/outputs/apk/release/app-release.apk "$apk"
    echo "Built $apk"
    ~/docker/files/bin/share "$(pwd)/$apk"
