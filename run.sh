#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "${ROOT}/BankingSystemAPI"

PORT="${SERVER_PORT:-8080}"
PROFILE="${SPRING_PROFILES_ACTIVE:-dev}"

if [[ "${PROFILE}" == "mysql" && -f "${ROOT}/.env" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "${ROOT}/.env"
  set +a
  export DB_URL DB_USERNAME DB_PASSWORD
fi

if pids=$(lsof -ti ":${PORT}" 2>/dev/null) && [[ -n "${pids}" ]]; then
  echo "Port ${PORT} is in use. Stopping existing listener(s): ${pids}"
  kill ${pids} 2>/dev/null || true
  sleep 1
  if lsof -ti ":${PORT}" >/dev/null 2>&1; then
    echo "Could not free port ${PORT}. Stop the process manually, or run: SERVER_PORT=8081 ./run.sh"
    exit 1
  fi
fi

echo "Starting banking API on port ${PORT} (profile: ${PROFILE})..."
mvn spring-boot:run \
  -Dspring-boot.run.profiles="${PROFILE}" \
  -Dspring-boot.run.arguments="--server.port=${PORT}"
