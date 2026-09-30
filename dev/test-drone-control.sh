#!/usr/bin/env bash
set -euo pipefail
root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
command -v kotlinc >/dev/null || {
    echo "Kotlin compiler required (1.9+); use the project toolchain." >&2
    exit 1
}
command -v java >/dev/null || {
    echo "Java required (project runtime: 21)." >&2
    exit 1
}
build="$(mktemp -d)"
trap 'rm -rf -- "$build"' EXIT
sources="$root/src/main/kotlin/com/atsuishio/superbwarfare/control"
checks="$root/dev/drone-control-tests"
mapfile -t fixtures < <(find "$checks/fixtures" -name '*.kt' -print | sort)
# Target 17 lets the isolated harness run with Kotlin 1.9; production still targets Java 21.
# Fixtures replace Minecraft/Fabric only for unit tests; this does NOT compile/package the mod.
kotlinc -Werror -jvm-target 17 "$sources/DroneControlPolicy.kt" "$sources/DroneControlAccess.kt" "$sources/DroneControlEvents.kt" "$checks/DroneControlPolicyChecks.kt" "$checks/DroneControlAdapterChecks.kt" "$checks/DroneControlSessionChecks.kt" "${fixtures[@]}" -include-runtime -d "$build/checks.jar"
java -cp "$build/checks.jar" review.DroneControlPolicyChecksKt
java -cp "$build/checks.jar" review.DroneControlAdapterChecksKt
java -cp "$build/checks.jar" review.DroneControlSessionChecksKt
