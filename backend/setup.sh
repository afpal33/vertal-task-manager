#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")"

if [ -f .env ]; then
  printf '%s\n' '.env already exists; leaving it unchanged.'
  printf '%s\n' 'Run: docker compose up --build'
  exit 0
fi

command -v openssl >/dev/null 2>&1 || {
  printf '%s\n' 'Error: openssl is required to generate local server credentials.' >&2
  exit 1
}

mkdir -p secrets
umask 077

openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out secrets/server-private-key.pem 2>/dev/null
openssl pkey -in secrets/server-private-key.pem -pubout -outform DER 2>/dev/null | base64 | tr -d '\n' > secrets/server-public-key.b64

POSTGRES_PASSWORD="$(openssl rand -hex 24)"
JWT_SECRET="$(openssl rand -hex 32)"

cat > .env <<EOF
POSTGRES_DB=vertal
POSTGRES_USER=vertal
POSTGRES_PASSWORD=$POSTGRES_PASSWORD
JWT_SECRET=$JWT_SECRET
JWT_EXPIRATION=3600000
SERVER_PUBLIC_KEY=$(cat secrets/server-public-key.b64)
SERVER_PRIVATE_KEY_PATH=/run/secrets/server-private-key
SERVER_PORT=8080
EOF

chmod 600 .env secrets/server-private-key.pem secrets/server-public-key.b64
printf '%s\n' 'Vertal is configured.'
printf '%s\n' 'Start it with: docker compose up --build'
printf '%s\n' 'Backend URL: http://localhost:8080'