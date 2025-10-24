#!/bin/bash

start_runtime() {
  local project_name="$1"
  local timeout_secs="${START_RUNTIME_TIMEOUT:-60}"

  # Resolve config and validate
  local config_dir="${PWD}/system-tests/config"
  local config_path="${config_dir}/${project_name}.properties"

  if [[ ! -f "$config_path" ]]; then
    echo "Config file not found: $config_path" >&2
    return 1
  fi

  local log_file="${project_name}.log"

  # Start Gradle with env and capture PID
  EDC_FS_CONFIG="$config_path" ./gradlew --no-daemon --console=plain "launchers:${project_name}:run" \
    > "$log_file" 2>&1 &
  local gradle_pid=$!
  export GRADLE_PID="$gradle_pid"
  echo "$project_name PID: $GRADLE_PID" >&2

  # Wait for readiness; on timeout, kill the Gradle process (and its children)
  if timeout "${timeout_secs}s" bash -c \
      'tail -n +1 -f "$1" | grep -m1 -qE "Runtime .* ready"' \
      bash "$log_file"; then
    echo "$gradle_pid"
  else
    echo "Timed out waiting for runtime readiness (${timeout_secs}s). Killing PID $gradle_pid..." >&2
    kill "$gradle_pid" 2>/dev/null || true
    sleep 2
    pkill -P "$gradle_pid" 2>/dev/null || true    # kill child processes
    kill -9 "$gradle_pid" 2>/dev/null || true     # force kill if still alive
    return 124
  fi

  # Return the PID
  echo "$gradle_pid"
}

# Extract a clean numeric PID (first number found), stripping CR/LF/whitespace
get_pid() {
  printf '%s' "$1" | tr -d '\r' | grep -oE '[0-9]+' | head -n1
}

safe_kill() {
  local raw="$1"
  local pid
  pid="$(get_pid "$raw")"
  [[ "$pid" =~ ^[0-9]+$ ]] || { echo "Skip invalid PID: '$raw'"; return 0; }

  # Try killing process group (if you used setsid), then the PID itself
  kill -TERM -- "-$pid" 2>/dev/null || true
  kill -TERM "$pid" 2>/dev/null || true
  sleep 2
  kill -KILL -- "-$pid" 2>/dev/null || true
  kill -KILL "$pid" 2>/dev/null || true
}
