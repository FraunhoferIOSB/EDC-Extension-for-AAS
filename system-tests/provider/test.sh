#!/bin/bash

set -euo pipefail

# Load utility functions
source "system-tests/util.sh"

# Step 0: Boot connector
start_runtime provider

MANAGEMENT_API="http://localhost:13338/management/v3"
catalog_request="$(cat system-tests/resources/request_data/catalog_request.json)"
query_spec="$(cat system-tests/resources/request_data/query_spec.json)"
################################ Step 1: Check catalog content ################################
verify_request "${MANAGEMENT_API}/catalog/request" "catalog" "POST" "$catalog_request"
################################ Step 2: Check asset store ################################
verify_request "${MANAGEMENT_API}/assets/request" "assets" "POST" "$query_spec"
################################ Step 3: Check contract store ################################
verify_request "${MANAGEMENT_API}/contractdefinitions/request" "contracts" "POST" "$query_spec"
################################ Step 4: Check policy store ################################
verify_request "${MANAGEMENT_API}/policydefinitions/request" "policy" "POST" "$query_spec"
################################ Step 5: Check Self Description ################################
SELF_DESCRIPTION_API="http://localhost:13337/api/selfDescription"
verify_request "${SELF_DESCRIPTION_API}" "self_description" "GET"

################################ Step 6: Negotiate and transfer data ################################
start_runtime consumer
CONSUMER_MANAGEMENT_API="http://localhost:23338/management/v3"
CONSUMER_CATALOG_API="${CONSUMER_MANAGEMENT_API}/catalog/request"
CONTRACT_NEGOTIATION_API="${CONSUMER_MANAGEMENT_API}/contractnegotiations"

# Step 6.1: Get offer ID
offer_id=$(curl -sS\
   --request POST\
   --url "${CONSUMER_CATALOG_API}"\
   --header "Content-Type: application/json"\
   --header "x-api-key: password" \
   --data "$catalog_request"\
   | jq -r '."dcat:dataset"[0]."odrl:hasPolicy"."@id"')

################################ Step 6.2: Send offer (from consumer to provider) ################################

export offer_id
contract_request="$(envsubst < system-tests/resources/request_data/contract_request.json)"

negotiation_id=$(curl -sS \
  --request POST \
  --url "$CONTRACT_NEGOTIATION_API" \
  --header "Content-Type: application/json" \
  --header "x-api-key: password" \
  --data "$contract_request" \
  | jq -r '."@id"')

################################ Step 6.3: Wait for agreement (30 seconds, else FAIL - this should be enough in a local scenario) ################################

deadline=$(( $(date +%s) + 30 ))
state=""

while (( $(date +%s) <= deadline )); do
  resp=$(curl -sS --fail "$CONTRACT_NEGOTIATION_API/$negotiation_id" -H "x-api-key: password") || {
    sleep 1
    continue
  }
  state=$(echo "$resp" | jq -r '.state // empty')
  if [[ "$state" == "FINALIZED" ]]; then
    # Continue with next steps
    echo "Negotiation succeeded"
    break
  fi
  sleep 0.5
done

if [[ "$state" != "FINALIZED" ]]; then
  echo "ERR: Timed out waiting for contract negotiation" >&2
  exit 1
fi

agreement_id=$(echo "$resp" | jq -r '.contractAgreementId')
echo "$agreement_id"
################################ Step 6.4: Using client extension, get data from agreement directly as response to check if data transfer works ################################
received_data=$(curl \
  --silent \
  --show-error \
  --request POST \
  --url "http://localhost:23337/api/automated/transfer?providerUrl=http%3A%2F%2Flocalhost%3A13340%2Fdsp&agreementId=$agreement_id" \
  --header "x-api-key: password" \
  --header "Content-Type: application/json" \
  | jq -S -r)

should_be_data=$(< system-tests/resources/aas.json jq -S -r '."conceptDescriptions"[0]')

diff -w <(echo "$received_data") <(echo "$should_be_data") > /dev/null

echo "Transferred data checks out. Test complete!"
