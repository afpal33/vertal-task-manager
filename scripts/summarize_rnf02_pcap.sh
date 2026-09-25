#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "Uso: bash scripts/summarize_rnf02_pcap.sh <PCAP> <DIRECTORIO_EVIDENCIA>" >&2
  exit 2
fi

pcap_path="$1"
evidence_dir="$2"
server_url="${RNF02_SERVER_URL:-http://10.0.2.2:8080}"
destinations_path="$evidence_dir/vertal-rnf02-destinations.txt"
packets_path="$evidence_dir/vertal-rnf02-packets.csv"
report_path="$evidence_dir/vertal-rnf02-report.txt"

if [[ ! -s "$pcap_path" ]]; then
  echo "La captura de paquetes de RNF02 no existe o está vacía: $pcap_path" >&2
  exit 1
fi

if ! command -v tshark >/dev/null 2>&1; then
  echo "Se requiere tshark para analizar la captura de paquetes de RNF02." >&2
  exit 1
fi

mkdir -p "$evidence_dir"

tshark -r "$pcap_path" -Y 'ip.dst' -T fields -e ip.dst \
  | sed '/^$/d' \
  | sort -u > "$destinations_path"

tshark -r "$pcap_path" -Y 'ip.src || ip.dst' -T fields \
  -E header=y -E separator=, -E quote=d \
  -e frame.time_epoch -e ip.src -e ip.dst -e ip.proto \
  -e tcp.srcport -e tcp.dstport > "$packets_path"

server_packet_count="$(
  tshark -r "$pcap_path" -Y 'ip.addr == 10.0.2.2 && tcp.port == 8080' \
    -T fields -e frame.number | wc -l | tr -d ' '
)"
first_packet_epoch="$(tshark -r "$pcap_path" -T fields -e frame.time_epoch | sed -n '1p')"
last_packet_epoch="$(tshark -r "$pcap_path" -T fields -e frame.time_epoch | tail -n 1)"
capture_duration_seconds="$(
  awk -v first="$first_packet_epoch" -v last="$last_packet_epoch" \
    'BEGIN { printf "%.3f", last - first }'
)"

if [[ "$server_packet_count" -eq 0 ]]; then
  echo "La captura no contiene paquetes relacionados con 10.0.2.2:8080." >&2
  exit 1
fi

{
  echo "Requisito: RNF02"
  echo "Commit: ${GITHUB_SHA:-ejecución local}"
  echo "Fecha de captura (UTC): $(date -u +'%Y-%m-%dT%H:%M:%SZ')"
  echo "Duración de la captura (segundos): $capture_duration_seconds"
  echo "Servidor configurado: $server_url"
  echo "Archivo de captura: $pcap_path"
  echo "Paquetes relacionados con el servidor configurado: $server_packet_count"
  echo "Archivo de destinos IP observados: $destinations_path"
  echo "Inventario de paquetes: $packets_path"
  echo "Resultado automatizado del cliente: la prueba de integración de privacidad se conectó al servidor configurado y rechazó una URL absoluta perteneciente a un tercero."
  echo "Límite de interpretación: la captura también contiene tráfico del sistema Android, que debe separarse durante la revisión manual antes de atribuir un destino a Vertal."
} > "$report_path"

cat "$report_path"
