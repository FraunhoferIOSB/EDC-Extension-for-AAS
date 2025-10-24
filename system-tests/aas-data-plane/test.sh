#!/bin/bash

# Load utility
SCRIPT_DIR="$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/../util.sh"

echo "" > consumer.log
control_plane_pid="$(start_runtime consumer)"

echo "" > aas-data-plane.log
data_plane_pid="$(start_runtime aas-data-plane)"

API="http://localhost:23339/control/v1/dataplanes"
# For more tests, this url can be useful
DP_URL="http://localhost:18235/control/v1/dataflows"

EXPECTED_FILE="system-tests/resources/dataplanes.json"

curl -fsS "$API" > dataplanes_is.log

python3 "system-tests/json_subset.py" "$EXPECTED_FILE" "dataplanes_is.log"

dataplanes_equality=$?

if [ "$dataplanes_equality" != 0 ];
then
    echo "Dataplanes do not match resources/dataplanes.json. Failing test"
    < dataplanes_is.log jq -r
    # Clean up
    safe_kill "$control_plane_pid"
    safe_kill "$data_plane_pid"
    exit 1
fi

# Clean up
safe_kill "$control_plane_pid"
safe_kill "$data_plane_pid"
