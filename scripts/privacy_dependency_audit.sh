#!/usr/bin/env bash
set -euo pipefail

audit_tmp="$(mktemp -d /tmp/vertal-privacy-audit-XXXXXX)"
trap 'rm -rf "$audit_tmp"' EXIT

(
  cd frontend
  flutter pub deps --style=compact
) >"$audit_tmp/frontend-dependencies.txt"

(
  cd backend
  ./mvnw dependency:tree -Dscope=runtime
) >"$audit_tmp/backend-dependencies.txt"

prohibited='firebase.analytics|firebase-crashlytics|sentry|appcenter|datadog|newrelic|new-relic|amplitude|mixpanel|segment-analytics'

if rg --ignore-case --line-number "$prohibited" \
  "$audit_tmp/frontend-dependencies.txt" \
  "$audit_tmp/backend-dependencies.txt"; then
  echo 'Auditoría fallida: se detectó una dependencia de telemetría o analítica.' >&2
  exit 1
fi

echo 'Auditoría de dependencias conforme.'
echo 'No se detectaron SDK conocidos de telemetría o analítica en los árboles de dependencias resueltos.'
