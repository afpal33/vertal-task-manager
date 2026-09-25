#!/usr/bin/env bash
set -euo pipefail

# Keep multiline commands and their exit statuses in one shell: the emulator
# action executes each line of its script input in a separate shell.
cd "$(dirname "${BASH_SOURCE[0]}")/../frontend"
mkdir -p build/test-reports

notification_status=0
privacy_status=0

flutter test integration_test/notification_delivery_test.dart \
  -d "${ANDROID_SERIAL:-emulator-5554}" \
  --reporter expanded \
  --file-reporter json:build/test-reports/notification-integration.json \
  || notification_status=$?

flutter test integration_test/privacy_android_test.dart \
  -d "${ANDROID_SERIAL:-emulator-5554}" \
  --reporter expanded \
  --file-reporter json:build/test-reports/privacy-android-integration.json \
  || privacy_status=$?

printf 'Android integration results: notification=%s privacy=%s\n' \
  "$notification_status" "$privacy_status"

if [ "$notification_status" -ne 0 ] || [ "$privacy_status" -ne 0 ]; then
  exit 1
fi
