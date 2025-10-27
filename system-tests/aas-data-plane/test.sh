#!/usr/bin/env bash

set -euo pipefail

# Load utility
SCRIPT_DIR="$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/../util.sh"

# Preserve original exit code after cleanup
trap 'rc=$?; cleanup; exit $rc' EXIT
# Also run cleanup on Ctrl+C/kill
trap 'cleanup; exit 130' INT
trap 'cleanup; exit 143' TERM

echo "" > consumer.log
export control_plane_pid
if ! control_plane_pid="$(start_runtime consumer)"; then
  rc=$?
  echo "start_runtime failed ($rc)"
  exit $rc
fi

echo "" > aas-data-plane.log
export data_plane_pid
if ! data_plane_pid="$(start_runtime aas-data-plane)"; then
  rc=$?
  echo "start_runtime failed ($rc)"
  exit $rc
fi

API="http://localhost:23339/control/v1/dataplanes"
# For more tests, this url can be useful
# DP_URL="http://localhost:18235/control/v1/dataflows"

EXPECTED_FILE="system-tests/resources/dataplanes.json"

curl -fsS "$API" > dataplanes_is.log

python3 "system-tests/json_subset.py" "$EXPECTED_FILE" "dataplanes_is.log"

dataplanes_equality=$?

if [ "$dataplanes_equality" != 0 ];
then
    echo "Dataplanes do not match resources/dataplanes.json. Failing test"
    exit 1
fi

echo "AAS data plane tests passed."
