#!/usr/bin/env bash

# Install exit traps -> kill processes to free network ports for the next test
if [[ -z "${__UTIL_TRAPS_INSTALLED:-}" ]]; then
  __UTIL_TRAPS_INSTALLED=1
  declare -a __UTIL_LAUNCHED_PIDS=()

  # Preserve original exit code; cleanup never makes the step fail
  trap '__rc=$?; cleanup_all || true; exit $__rc' EXIT
  trap 'cleanup_all || true; exit 130' INT
  trap 'cleanup_all || true; exit 143' TERM
fi

track_launch() {
  local pid="$1"
  [[ "$pid" =~ ^[0-9]+$ ]] || return 0
  __UTIL_LAUNCHED_PIDS+=("$pid")
}

start_runtime() {
  local project_name="$1"
  local timeout_secs="${START_RUNTIME_TIMEOUT:-120}"

  local config_path="${PWD}/system-tests/config/${project_name}.properties"

  if [[ ! -f "$config_path" ]]; then
    echo "ERR: Config file not found: $config_path" >&2
    exit 1
  fi

  local log_file="${project_name}.log"

  echo "Starting ${project_name}..." >&2
  EDC_FS_CONFIG="$config_path" "${PWD}/gradlew" --no-daemon --console=plain "launchers:${project_name}:run" \
    > "$log_file" 2>&1 &
  local pid=$!

  if timeout "${timeout_secs}s" bash -c \
      'tail -n +1 -f "$1" | grep -m1 -qE "Runtime .* ready"' bash "$log_file"; then
    track_launch "$pid"
    return 0
  else
    echo "ERR: Timed out waiting for runtime readiness (${timeout_secs}s). Killing PID $pid..." >&2
    cat "$log_file" >&2
    track_launch "$pid"
    cleanup_pid "$pid" || true
    exit 124
  fi
}

cleanup_pid() {
  local raw="$1"
  [[ "$raw" =~ ^[0-9]+$ ]] || return 0
  kill -TERM "$raw" 2>/dev/null || true
  sleep 2
  kill -KILL "$raw" 2>/dev/null || true
}

cleanup_all() {
  echo "Cleaning up..."
  for p in "${__UTIL_LAUNCHED_PIDS[@]}"; do
    cleanup_pid "$p" || true
  done
}

verify_request() {
  local url="$1"
  local resource_name="$2"
  local method="${3:-POST}"
  local body="${4:-}"

  local log_file="${resource_name}_is.log"

  local curl_args=(
    --silent
    --show-error
    --output "$log_file"
    --write-out '%{http_code}'
    --request "$method"
    --url "$url"
    --header "x-api-key: password"
  )

  if [[ -n "$body" ]]; then
    curl_args+=(--data "$body" --header "Content-Type: application/json")
  fi

  http_code=$(curl "${curl_args[@]}")

  if [[ "$http_code" != 2?? ]]; then
    echo "ERR: $resource_name: $method request returned HTTP $http_code. Failing test and dumping actual response."
    cat "$log_file" >&2
    exit 1
  fi

if ! python3 'system-tests/json_subset.py' "system-tests/resources/${resource_name}.json" "${log_file}";
  then
      echo "ERR: $resource_name: Response JSON does not match expected. Failing test and dumping actual response."
      jq < "$log_file" >&2
      printf "\n"
      exit 1
  fi
  echo "$resource_name matches expected."
}
