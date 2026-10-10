#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
bash gradlew connectedDebugAndroidTest
mkdir -p app/build/reports/device-preview
for stage in 0 1 2 3; do
  adb exec-out run-as com.eightbittrade.clipforge cat "files/qa-step-$stage.png" > "app/build/reports/device-preview/step-$stage.png"
done
adb exec-out run-as com.eightbittrade.clipforge cat cache/pipeline-portrait.mp4 > app/build/reports/device-preview/portrait.mp4
