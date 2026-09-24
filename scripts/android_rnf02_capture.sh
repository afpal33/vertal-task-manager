#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 3 ]]; then
  echo "Uso: bash scripts/android_rnf02_capture.sh <AVD> <URL_SERVIDOR> <DIRECTORIO_EVIDENCIA>" >&2
  exit 2
fi

avd_name="$1"
server_url="$2"
evidence_dir="$3"
emulator_bin="$(command -v emulator || true)"
adb_bin="$(command -v adb || true)"
tshark_bin="$(command -v tshark || true)"

if [[ -z "$emulator_bin" || -z "$adb_bin" || -z "$tshark_bin" ]]; then
  echo "Se requieren emulator, adb y tshark para generar la evidencia RNF02." >&2
  exit 1
fi

mkdir -p "$evidence_dir"
pcap_path="$evidence_dir/vertal-rnf02.pcap"
destinations_path="$evidence_dir/vertal-rnf02-destinations.txt"
report_path="$evidence_dir/vertal-rnf02-report.txt"

"$emulator_bin" "@$avd_name" \
  -no-window \
  -no-audio \
  -no-boot-anim \
  -wipe-data \
  -tcpdump "$pcap_path" &
emulator_pid=$!

cleanup() {
  "$adb_bin" emu kill >/dev/null 2>&1 || true
  wait "$emulator_pid" 2>/dev/null || true
}
trap cleanup EXIT

"$adb_bin" wait-for-device
for _ in $(seq 1 120); do
  if [[ "$("$adb_bin" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; then
    break
  fi
  sleep 1
done

device_serial="$("$adb_bin" devices | awk '$1 ~ /^emulator-/ && $2 == "device" {print $1; exit}')"
if [[ -z "$device_serial" ]]; then
  echo "El emulador no terminó de iniciar." >&2
  exit 1
fi

(
  cd frontend
  flutter test integration_test/privacy_android_test.dart \
    -d "$device_serial" \
    --dart-define="RNF02_SERVER_URL=$server_url"
)

cleanup
trap - EXIT

"$tshark_bin" -r "$pcap_path" -Y 'ip.dst' -T fields -e ip.dst \
  | sed '/^$/d' \
  | sort -u > "$destinations_path"

{
  echo "Requisito: RNF02"
  echo "Servidor configurado: $server_url"
  echo "AVD: $avd_name"
  echo "Captura: $pcap_path"
  echo "Destinos observados: $destinations_path"
  echo "Resultado de la prueba Flutter: conforme"
  echo "Revisión requerida: separar el tráfico del sistema Android y confirmar que Vertal solo se comunicó con el servidor configurado."
} > "$report_path"

echo "Evidencia RNF02 generada en $evidence_dir"
