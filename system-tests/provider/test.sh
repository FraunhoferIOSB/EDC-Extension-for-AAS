#!/bin/bash

set -euo pipefail
# Load utility
SCRIPT_DIR="$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/../util.sh"

# Step 0: Boot connector
echo "" > provider.log
provider_pid="$(start_runtime provider)"


MANAGEMENT_API="http://localhost:13338/management/v3"

# Step 1: Check catalog content
CATALOG_API="$MANAGEMENT_API/catalog/request"

curl -fsS -X POST\
  $CATALOG_API\
   -H "Content-Type: application/json" -H "x-api-key: password" \
   -d '
   {
    "@context": {
      "edc": "https://w3id.org/edc/v0.0.1/ns/"
    },
    "counterPartyAddress": "http://localhost:13340/dsp",
    "protocol": "dataspace-protocol-http",
    "querySpec": {}
   }
  ' > catalog_is.log

python3 "system-tests/json_subset.py" "system-tests/resources/catalog.json" "catalog_is.log"

catalog_equality=$?

if [ "$catalog_equality" != 0 ];
then
    echo "Catalog does not match resources/catalog.json. Failing test"
    exit 1
fi

echo "Catalog matches expected."

# Step 2: Check asset store
ASSET_API="$MANAGEMENT_API/assets/request"

curl -fsS -X POST\
  $ASSET_API\
   -H "Content-Type: application/json" -H "x-api-key: password" \
   -d '
   {
     "@context": {
       "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
       "aas": "https://admin-shell.io/aas/3/0/"
     },
     "@type": "QuerySpec",
     "limit": 31415926
   }
  ' > assets_is.log

python3 "system-tests/json_subset.py" "system-tests/resources/assets.json" "assets_is.log"

asset_equality=$?

if [ "$asset_equality" != 0 ];
then
    echo "Assets do not match resources/assets.json. Failing test"
    exit 1
fi

echo "Assets match expected."

# Step 3: Check contract store
CONTRACTS_API="$MANAGEMENT_API/contractdefinitions/request"

curl -fsS -X POST\
  $CONTRACTS_API\
   -H "Content-Type: application/json" -H "x-api-key: password" \
   -d '
   {
     "@context": {
       "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
       "aas": "https://admin-shell.io/aas/3/0/"
     },
     "@type": "QuerySpec",
     "limit": 31415926
   }
  ' > contracts_is.log

python3 "$SCRIPT_DIR/../json_subset.py" "$SCRIPT_DIR/../resources/contracts.json" "contracts_is.log"

contract_equality=$?

if [ "$contract_equality" != 0 ];
then
    echo "Contracts do not match resources/contracts.json. Failing test"
    exit 1
fi

echo "Contracts match expected."

# Step 4: Check policy store
POLICY_API="$MANAGEMENT_API/policydefinitions/request"

curl -fsS -X POST\
  $POLICY_API\
   -H "Content-Type: application/json" -H "x-api-key: password" \
   -d '
   {
     "@context": {
       "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
       "aas": "https://admin-shell.io/aas/3/0/"
     },
     "@type": "QuerySpec",
     "limit": 31415926
   }
  ' > policy_is.log

python3 "$SCRIPT_DIR/../json_subset.py" "$SCRIPT_DIR/../resources/policy.json" "policy_is.log"

policy_equality=$?

if [ "$policy_equality" != 0 ];
then
    echo "Policies do not match resources/policy.json. Failing test"
    exit 1
fi

echo "Policies match expected."

# Step 5: Boot consumer
echo "" > consumer.log
consumer_pid="$(start_runtime consumer)"

# Step 6: Negotiate and transfer data
CONSUMER_MANAGEMENT_API="http://localhost:23338/management/v3"

# Step 6.1: Get offer ID
offer_id=$(curl -sS -X POST http://localhost:23338/management/v3/catalog/request -H 'Content-Type: application/json' -H 'x-api-key: password' -d '{
    "@context": {
        "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
    },
    "counterPartyAddress": "http://localhost:13340/dsp",
    "protocol": "dataspace-protocol-http"
}' | jq -r '."dcat:dataset"[0]."odrl:hasPolicy"."@id"')

# Step 6.2: Send offer (from consumer to provider)

CONTRACT_NEGOTIATION_API="$CONSUMER_MANAGEMENT_API/contractnegotiations"

negotiation_id=$(curl -sS -X POST $CONTRACT_NEGOTIATION_API -H 'x-api-key: password' -H 'Content-Type: application/json'\
  -d "{
        \"@type\": \"ContractRequest\",
        \"protocol\": \"dataspace-protocol-http\",
        \"counterPartyAddress\": \"http://localhost:13340/dsp\",
        \"policy\": {
          \"@context\": \"http://www.w3.org/ns/odrl.jsonld\",
          \"@type\": \"odrl:Offer\",
          \"@id\": \"$offer_id\",
          \"permission\": {
            \"odrl:action\": {
              \"@id\": \"odrl:use\"
            }
          },
          \"prohibition\": [],
          \"obligation\": [],
          \"assigner\": \"provider\",
          \"target\": \"-824113802\"
        },
        \"callbackAddresses\": [],
        \"@context\": {
          \"aas\": \"https://admin-shell.io/aas/3/0/\",
          \"@vocab\": \"https://w3id.org/edc/v0.0.1/ns/\",
          \"edc\": \"https://w3id.org/edc/v0.0.1/ns/\",
          \"odrl\": \"http://www.w3.org/ns/odrl/2/\"
        }
      }" | jq -r '."@id"')

# Step 6.3: Wait for agreement (30 seconds, else FAIL - this should be enough in a local scenario)

deadline=$(( $(date +%s) + 30 ))
state=""

while (( $(date +%s) <= deadline )); do
  resp=$(curl --no-progress-meter --fail "$CONTRACT_NEGOTIATION_API/$negotiation_id" -H "x-api-key: password") || {
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
  echo "Timed out waiting for contract negotiation" >&2
  exit 1
fi

agreement_id=$(echo "$resp" | jq -r '.contractAgreementId')

# Step 6.4: Using client extension, get data from agreement directly as response to check if data transfer works

received_data=$(curl -sS -X POST "http://localhost:23337/api/automated/transfer?providerUrl=http%3A%2F%2Flocalhost%3A13340%2Fdsp&agreementId=$agreement_id" | jq -S -r)

should_be_data=$(< system-tests/resources/aas.json jq -S -r '."submodels"[0]."submodelElements"[0]')

diff -w <(echo "$received_data") <(echo "$should_be_data")

echo "Transferred data checks out. Test complete!"

# Step N: clean up running connectors

safe_kill "$provider_pid"
safe_kill "$consumer_pid"