#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/banking-frontend"

if [[ ! -d node_modules ]]; then
  echo "Installing frontend dependencies..."
  npm install
fi

echo "Starting frontend at http://localhost:5173"
echo "Ensure the API is running on http://localhost:8080 (./run.sh or ./run-mysql.sh)"
npm run dev
