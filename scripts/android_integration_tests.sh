#!/usr/bin/env bash
set -euo pipefail

# Keep multiline commands and their exit statuses in one shell: the emulator
# action executes each line of its script input in a separate shell.
cd "$(dirname "${BASH_SOURCE[0]}")/../frontend"
mkdir -p build/test-reports

notification_status=0
privacy_status=0
backend_pid=""
server_url="${RNF02_SERVER_URL:-http://10.0.2.2:8080}"

cleanup() {
  if [ -n "$backend_pid" ]; then
    pkill -TERM -P "$backend_pid" 2>/dev/null || true
    kill "$backend_pid" 2>/dev/null || true
    wait "$backend_pid" 2>/dev/null || true
  fi
}
trap cleanup EXIT

# The Android emulator reaches services on the runner host through 10.0.2.2.
# Run the real Spring Boot application with its test profile so the privacy
# test produces observable client-to-server traffic without requiring an
# external service or a persistent PostgreSQL instance.
(
  cd ../backend
  exec ./mvnw --batch-mode \
    -Dspring-boot.run.profiles=test \
    -Dspring-boot.run.useTestClasspath=true \
    spring-boot:run
) > build/test-reports/rnf02-backend.log 2>&1 &
backend_pid=$!

backend_ready=0
for _ in $(seq 1 120); do
  if curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health \
    > build/test-reports/rnf02-backend-health.json; then
    backend_ready=1
    break
  fi
  if ! kill -0 "$backend_pid" 2>/dev/null; then
    break
  fi
  sleep 1
done

if [ "$backend_ready" -ne 1 ]; then
  echo "El backend requerido para RNF02 no inició correctamente." >&2
  cat build/test-reports/rnf02-backend.log >&2
  exit 1
fi

flutter test integration_test/notification_delivery_test.dart \
  -d "${ANDROID_SERIAL:-emulator-5554}" \
  --reporter expanded \
  --file-reporter json:build/test-reports/notification-integration.json \
  || notification_status=$?

flutter test integration_test/privacy_android_test.dart \
  -d "${ANDROID_SERIAL:-emulator-5554}" \
  --dart-define="RNF02_SERVER_URL=$server_url" \
  --reporter expanded \
  --file-reporter json:build/test-reports/privacy-android-integration.json \
  || privacy_status=$?

printf 'Android integration results: notification=%s privacy=%s\n' \
  "$notification_status" "$privacy_status"

if [ "$notification_status" -ne 0 ] || [ "$privacy_status" -ne 0 ]; then
  exit 1
fi
