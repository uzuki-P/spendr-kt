# List available recipes
default:
    @just --list

# Build, install, and launch the debug app on the connected emulator/device
dev:
    ./gradlew :app:installMainAppDebug
    adb shell monkey -p com.spendr.app.kt 1

# Full validation: debug APK, lint, and JVM unit tests
check:
    ./gradlew :app:assembleMainAppDebug :app:assembleDevDebug :app:lintMainAppDebug :app:lintDevDebug :app:testMainAppDebugUnitTest :app:testDevDebugUnitTest

# Build the signed release APK into _apk/spendr-kt-<version>-<d.mmm-HH-MM>.apk and upload it
build-apk:
    #!/usr/bin/env bash
    set -euo pipefail
    ./gradlew :app:assembleMainAppRelease
    mkdir -p _apk
    ver="$(sed -n 's/.*versionName = "\(.*\)"/\1/p' app/build.gradle.kts | head -1)"
    stamp="$(date +"%-d.%b-%H-%M" | tr 'A-Z' 'a-z')"
    apk="_apk/spendr-kt-${ver}-${stamp}.apk"
    cp app/build/outputs/apk/mainApp/release/app-mainApp-release.apk "$apk"
    echo "Built $apk"
    ~/docker/files/bin/share "$(pwd)/$apk"

# Build the dev debug APK and stage a timestamped DEV copy in _apk
build-apk-dev:
    #!/usr/bin/env bash
    set -euo pipefail
    ./gradlew :app:assembleDevDebug
    mkdir -p _apk
    ver="$(sed -n 's/.*versionName = "\(.*\)"/\1/p' app/build.gradle.kts | head -1)"
    stamp="$(date +"%d-%b_%H-%M" | tr 'A-Z' 'a-z')"
    apk="_apk/DEV-spendr-kt-${ver}-dev_${stamp}.apk"
    cp app/build/outputs/apk/dev/debug/app-dev-debug.apk "$apk"
    echo "Built $apk"
