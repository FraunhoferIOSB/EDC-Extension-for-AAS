#!/usr/bin/env bash

set -euo pipefail

# always run from the repository root, no matter where the script was invoked from
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT_DIR}"

# Load utility functions (verify_request)
source "system-tests/util.sh"

COMPOSE_FILE="system-tests/aas-data-plane/docker-compose.yaml"
AAS_DATA_PLANE_IMAGE="${AAS_DATA_PLANE_IMAGE:-aas-data-plane:latest}"
CONSUMER_IMAGE="${CONSUMER_IMAGE:-consumer:latest}"

# docker compose v2 plugin, else the legacy standalone docker-compose binary
if docker compose version > /dev/null 2>&1; then
    COMPOSE=(docker compose)
elif command -v docker-compose > /dev/null 2>&1; then
    COMPOSE=(docker-compose)
else
    echo "ERR: Neither 'docker compose' nor 'docker-compose' is available." >&2
    exit 1
fi
COMPOSE+=(-f "${COMPOSE_FILE}")

dump_logs() {
    "${COMPOSE[@]}" logs --no-color aas-data-plane > system-tests/aas-data-plane/logs/aas-data-plane.log 2>&1 || true
    "${COMPOSE[@]}" logs --no-color consumer > system-tests/aas-data-plane/logs/consumer.log 2>&1 || true
}

shutdown() {
    "${COMPOSE[@]}" logs consumer || true
    "${COMPOSE[@]}" logs aas-data-plane || true
    local exit_code=$?
    cleanup_all > /dev/null 2>&1 || true
    dump_logs
    "${COMPOSE[@]}" down --volumes --remove-orphans > /dev/null 2>&1 || true
    exit "${exit_code}"
}
trap shutdown EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

mkdir -p system-tests/aas-data-plane/logs

if [[ -n "${SKIP_IMAGE_BUILD:-}" ]]; then
    echo "SKIP_IMAGE_BUILD is set; assuming images ${AAS_DATA_PLANE_IMAGE} and ${CONSUMER_IMAGE} are up to date"
else
    ./gradlew --no-daemon --console=plain launchers:aas-data-plane:shadowJar launchers:consumer:shadowJar

    docker build -t "${AAS_DATA_PLANE_IMAGE}" -f launchers/Dockerfile \
        --build-arg PROJECT=aas-data-plane --build-arg "ADDITIONAL_FILES=build/legal/*" \
        launchers/aas-data-plane
    docker build -t "${CONSUMER_IMAGE}" -f launchers/Dockerfile \
        --build-arg PROJECT=consumer --build-arg "ADDITIONAL_FILES=build/legal/*" \
        launchers/consumer
fi

"${COMPOSE[@]}" up -d --wait --wait-timeout "${START_RUNTIME_TIMEOUT:-300}" --force-recreate aas-data-plane
echo "aas-data-plane is ready."
"${COMPOSE[@]}" up -d --wait --wait-timeout "${START_RUNTIME_TIMEOUT:-300}" --force-recreate consumer
echo "consumer is ready."


API="http://localhost:23339/control/v1/dataplanes"
# For more tests, this url can be useful
# DP_URL="http://localhost:18235/control/v1/dataflows"

EXPECTED_FILE="system-tests/aas-data-plane/resources/dataplanes.json"

curl -fsS "$API" > system-tests/aas-data-plane/logs/dataplanes_is.log

python3 "system-tests/json_subset.py" "$EXPECTED_FILE" "system-tests/aas-data-plane/logs/dataplanes_is.log"

dataplanes_equality=$?

if [ "$dataplanes_equality" != 0 ];
then
    echo "Dataplanes do not match resources/dataplanes.json. Failing test"
    exit 1
fi

echo "AAS data plane tests passed."
