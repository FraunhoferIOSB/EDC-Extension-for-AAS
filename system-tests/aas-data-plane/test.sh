#!/usr/bin/env bash

set -euo pipefail

# Load utility functions
source "system-tests/util.sh"

# Start both connectors
start_runtime consumer
start_runtime aas-data-plane

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
