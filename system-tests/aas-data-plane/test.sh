#!/bin/bash

# Load utility
SCRIPT_DIR="$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/../util.sh"

echo "" > consumer.log

control_plane_pid="$(start_runtime consumer)"

echo "" > aas-data-plane.log

data_plane_pid="$(start_runtime aas-data-plane)"

API="http://localhost:9191/control/v1/dataplanes"
DP_URL="http://localhost:18235/control/v1/dataflows"

# These are the expected fields
EXPECTED_FILE="${SCRIPT_DIR}/dataplane_requiredfields.json"

export EXPECTED_FILE API DP_URL

# Check if data plane has self-registered and provides AAS (and HTTP) functionality
# CURL control-plane, parse response with jq, remove variable fields (e.g., @id), match rest
if curl -fsS "$API" | jq -e --slurpfile exp "$EXPECTED_FILE" '
  def n:
    del(.["@id"], .lastActive, .stateTimestamp)
    | {
        "@type": .["@type"],
        "url": .url,
        "allowedSourceTypes": ((.allowedSourceTypes // []) | sort),
        "allowedTransferTypes": ((.allowedTransferTypes // []) | sort)
      };
  ($exp[0] | n) as $e
  | [ .[] | n | select(. == $e) ]
  | length == 1
' >> /dev/null; then
 echo "Dataplane instance matches. Test succeeded..."
else
 echo "Dataplane instance missing or mismatched. Failing." >&2
 exit 1
fi

# Clean up
safe_kill "$control_plane_pid"
safe_kill "$data_plane_pid"
