#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
server_root="${BUKMAN_TEST_SERVER_DIR:-$repo_root/.testserver/purpur-experimental}"

if [[ ! -f "$server_root/purpur.jar" ]]; then
  echo "[run] Purpur jar not found in $server_root. Running setup first..."
  "$repo_root/scripts/purpur-experimental-setup.sh"
fi

if [[ ! -x "$server_root/start.sh" ]]; then
  echo "[run] start.sh missing or not executable. Running setup first..."
  "$repo_root/scripts/purpur-experimental-setup.sh"
fi

echo "[run] Starting Purpur experimental test server from: $server_root"
exec "$server_root/start.sh"
