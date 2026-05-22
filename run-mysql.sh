#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="${ROOT}/.env"
PLACEHOLDER_PASSWORD="your_mysql_password_here"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing .env — run ./setup-mysql.sh first."
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "${ENV_FILE}"
set +a

if [[ -z "${DB_PASSWORD:-}" || "${DB_PASSWORD}" == "${PLACEHOLDER_PASSWORD}" ]]; then
  echo "MySQL password not set in .env. Run ./setup-mysql.sh to configure it."
  exit 1
fi

export SPRING_PROFILES_ACTIVE=mysql
exec "${ROOT}/run.sh"
