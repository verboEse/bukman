#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
server_root="${BUKMAN_TEST_SERVER_DIR:-$repo_root/.testserver/purpur-experimental}"
purpur_version="${PURPUR_MC_VERSION:-26.1.2}"
purpur_url="https://api.purpurmc.org/v2/purpur/${purpur_version}/latest/download"

build_plugin="${BUILD_PLUGIN_JAR:-1}"

mkdir -p "$server_root/plugins"

if [[ "$build_plugin" == "1" ]]; then
  echo "[setup] Building Bukkit shadow jar..."
  (cd "$repo_root" && ./gradlew :Bukkit:shadowJar)
fi

echo "[setup] Resolving Bukman Bukkit shadow jar..."
plugin_jar="$(ls -t "$repo_root"/Bukkit/build/libs/Bukman-Bukkit-*-all.jar 2>/dev/null | head -n 1 || true)"
if [[ -z "$plugin_jar" ]]; then
  echo "[setup] ERROR: Could not find shadow jar in Bukkit/build/libs/."
  echo "[setup] Run './gradlew :Bukkit:shadowJar' and try again."
  exit 1
fi

echo "[setup] Downloading latest Purpur experimental build for ${purpur_version}..."
curl -fsSL "$purpur_url" -o "$server_root/purpur.jar"

cat >"$server_root/eula.txt" <<'EOF'
eula=true
EOF

cp "$plugin_jar" "$server_root/plugins/Bukman.jar"

mkdir -p "$server_root/plugins/Bukman"
cat >"$server_root/plugins/Bukman/config.yml" <<'EOF'
settings:
  debug-commands: true
EOF

cat >"$server_root/start.sh" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

java_bin="${JAVA_BIN:-/opt/homebrew/opt/openjdk@25/bin/java}"
java_args="${JAVA_ARGS:--Xms1G -Xmx2G}"

exec "$java_bin" $java_args -jar purpur.jar nogui
EOF

chmod +x "$server_root/start.sh"

echo "[setup] Test server prepared in: $server_root"
echo "[setup] Installed plugin jar: $plugin_jar"
echo "[setup] Start server with: $server_root/start.sh"
echo "[setup] Command tracing is enabled in: $server_root/plugins/Bukman/config.yml"
