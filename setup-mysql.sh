#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="${ROOT}/.env"
PLACEHOLDER_PASSWORD="your_mysql_password_here"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Creating ${ENV_FILE} from .env.example..."
  cp "${ROOT}/.env.example" "${ENV_FILE}"
fi

set -a
# shellcheck disable=SC1090
source "${ENV_FILE}"
set +a

if [[ -z "${DB_USERNAME:-}" ]]; then
  echo "Set DB_USERNAME in .env"
  exit 1
fi

if [[ -z "${DB_PASSWORD:-}" || "${DB_PASSWORD}" == "${PLACEHOLDER_PASSWORD}" ]]; then
  echo "MySQL password not configured in .env."
  echo -n "Enter password for MySQL user '${DB_USERNAME}': "
  read -rs DB_PASSWORD
  echo ""
  if [[ -z "${DB_PASSWORD}" ]]; then
    echo "Password cannot be empty."
    exit 1
  fi
  tmp_env="$(mktemp)"
  grep -v '^DB_PASSWORD=' "${ENV_FILE}" > "${tmp_env}" || true
  printf 'DB_PASSWORD=%s\n' "${DB_PASSWORD}" >> "${tmp_env}"
  mv "${tmp_env}" "${ENV_FILE}"
  echo "Saved DB_PASSWORD to .env"
fi

HOST_PORT="${DB_URL#jdbc:mysql://}"
HOST_PORT="${HOST_PORT%%/*}"
DB_NAME="${DB_URL##*/}"
DB_NAME="${DB_NAME%%\?*}"
MYSQL_HOST="${HOST_PORT%%:*}"
MYSQL_PORT="${HOST_PORT##*:}"
if [[ "${MYSQL_HOST}" == "${MYSQL_PORT}" ]]; then
  MYSQL_PORT="3306"
fi

echo "Testing MySQL connection (${DB_USERNAME}@${MYSQL_HOST}:${MYSQL_PORT})..."
if ! mysql -h "${MYSQL_HOST}" -P "${MYSQL_PORT}" -u "${DB_USERNAME}" -p"${DB_PASSWORD}" -e "SELECT 1" >/dev/null 2>&1; then
  echo "ERROR: Cannot connect to MySQL. Check DB_USERNAME / DB_PASSWORD in .env"
  echo "Tip: Use ./run.sh for H2 dev mode without MySQL."
  exit 1
fi

echo "Ensuring database '${DB_NAME}' exists..."
mysql -h "${MYSQL_HOST}" -P "${MYSQL_PORT}" -u "${DB_USERNAME}" -p"${DB_PASSWORD}" \
  -e "CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\`"

echo "MySQL connection ready. Schema is managed by Flyway — it will apply"
echo "db/migration on the next application start. Start the API with: ./run-mysql.sh"
