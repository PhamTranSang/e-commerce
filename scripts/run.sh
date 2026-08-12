#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

ENV_DIR="$PROJECT_ROOT/environment"
ENV_FILE="$ENV_DIR/application.yaml"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "ERROR: missing external config: $ENV_FILE" >&2
  echo "Create it with your local datasource url/username/password." >&2
  exit 1
fi

echo "Building bootable jar..."
./gradlew bootJar -q

# The bootable jar is the one that is NOT the *-plain.jar.
JAR="$(find build/libs -maxdepth 1 -name '*.jar' -not -name '*-plain.jar' 2>/dev/null | head -1)"
if [[ -z "$JAR" ]]; then
  echo "ERROR: no bootable jar found under build/libs/ after build." >&2
  exit 1
fi

# Pick a Java >= 25 runtime; the host default may be older (else the jar won't run).
java_major() { "$1" -version 2>&1 | awk -F'"' '/version/{print $2}' | cut -d. -f1; }
pick_java() {
  local candidates=()
  [[ -n "${JAVA_HOME:-}" ]] && candidates+=("$JAVA_HOME/bin/java")
  candidates+=(/usr/lib/jvm/*25*/bin/java "$HOME"/.gradle/jdks/*/bin/java)
  candidates+=("$(command -v java || true)")
  local c major
  for c in "${candidates[@]}"; do
    [[ -x "$c" ]] || continue
    major="$(java_major "$c")"
    [[ "$major" =~ ^[0-9]+$ && "$major" -ge 25 ]] && { echo "$c"; return 0; }
  done
  return 1
}
JAVA_BIN="$(pick_java || true)"
if [[ -z "$JAVA_BIN" ]]; then
  echo "ERROR: no Java >= 25 runtime found (the jar is built for Java 25)." >&2
  echo "Install JDK 25 or set JAVA_HOME to one." >&2
  exit 1
fi

DEBUG_ADDR="${DEBUG_ADDRESS:-127.0.0.1:${DEBUG_PORT:-8081}}"
echo "Starting: app http://localhost:8080 | remote debug ${DEBUG_ADDR}"
exec "$JAVA_BIN" \
  "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=${DEBUG_ADDR}" \
  -jar "$JAR" \
  --spring.config.additional-location=file:"$ENV_DIR/"
