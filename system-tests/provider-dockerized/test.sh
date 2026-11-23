#!/bin/bash

set -euo pipefail

# always run from the repository root, no matter where the script was invoked from
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT_DIR}"

# Load utility functions (verify_request)
source "system-tests/util.sh"

COMPOSE_FILE="system-tests/provider-dockerized/docker-compose.yaml"
PROVIDER_IMAGE="${PROVIDER_IMAGE:-provider:latest}"
CONSUMER_IMAGE="${CONSUMER_IMAGE:-consumer:latest}"
PROVIDER_DSP_HOST="provider"  # docker-compose service name of the provider connector

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
    "${COMPOSE[@]}" logs --no-color provider > logs/provider.log 2>&1 || true
    "${COMPOSE[@]}" logs --no-color consumer > logs/consumer.log 2>&1 || true
}

shutdown() {
    local exit_code=$?
    cleanup_all > /dev/null 2>&1 || true
    dump_logs
    "${COMPOSE[@]}" down --volumes --remove-orphans > /dev/null 2>&1 || true
    exit "${exit_code}"
}
trap shutdown EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

# The provider container keeps its 'localhost' identity (see config/provider.properties for
# why: the AAS asset IDs are hashes of the localhost-based FA(3)ST and DSP URLs, so the shared
# expected resources in system-tests/resources match unmodified). Everything the provider
# itself resolves stays on localhost. The only URLs that must change are the counterPartyAddress
# entries the CONSUMER uses to reach the provider; they are rewritten to the compose service
# name for the requests that go through the consumer.
to_compose_url() (
    sed "s#http://localhost:13340#http://${PROVIDER_DSP_HOST}:13340#g"
)

mkdir -p logs

######################################## Step -1: Build the connector images from the checked-out sources ########################################
# Same launchers/Dockerfile and image tags that './gradlew dockerize' produces. No build-file
# patching is involved: the images are built from the launcher sources as they are checked out.
if [[ -n "${SKIP_IMAGE_BUILD:-}" ]]; then
    echo "SKIP_IMAGE_BUILD is set; assuming images ${PROVIDER_IMAGE} and ${CONSUMER_IMAGE} are up to date"
else
    ./gradlew --no-daemon --console=plain launchers:provider:shadowJar launchers:consumer:shadowJar

    docker build -t "${PROVIDER_IMAGE}" -f launchers/Dockerfile \
        --build-arg PROJECT=provider --build-arg "ADDITIONAL_FILES=build/legal/*" \
        launchers/provider
    docker build -t "${CONSUMER_IMAGE}" -f launchers/Dockerfile \
        --build-arg PROJECT=consumer --build-arg "ADDITIONAL_FILES=build/legal/*" \
        launchers/consumer
fi

MANAGEMENT_API="http://localhost:13338/management/v4"
catalog_request="$(cat system-tests/resources/request_data/catalog_request.json)"
query_spec="$(cat system-tests/resources/request_data/query_spec.json)"

######################################## Step 0: Boot connectors ########################################
# --force-recreate: always start fresh connectors; in-memory state (vault, negotiated
# contracts/agreements) would otherwise leak between runs.
# --wait: block until the provider healthcheck passes (see docker-compose.yaml).
"${COMPOSE[@]}" up -d --wait --wait-timeout "${START_RUNTIME_TIMEOUT:-300}" --force-recreate provider
echo "Provider is ready."

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
"${COMPOSE[@]}" up -d --wait --wait-timeout "${START_RUNTIME_TIMEOUT:-300}" --force-recreate consumer
echo "Consumer is ready."
CONSUMER_MANAGEMENT_API="http://localhost:23338/management/v4"
CONSUMER_CATALOG_API="${CONSUMER_MANAGEMENT_API}/catalog/request"
CONTRACT_NEGOTIATION_API="${CONSUMER_MANAGEMENT_API}/contractnegotiations"

# Step 6.1: Get offer ID for the ConceptDescription
offer_id=$(curl -sS\
   --request POST\
   --url "${CONSUMER_CATALOG_API}"\
   --header "Content-Type: application/json"\
   --header "x-api-key: password" \
   --data "$(to_compose_url < system-tests/resources/request_data/catalog_request.json)"\
   | jq -r '.dataset[] | select(."@id" | startswith("680259192")) | .hasPolicy[0]."@id"')

################################ Step 6.2: Send offer (from consumer to provider) ################################

export offer_id
contract_request="$(to_compose_url < system-tests/resources/request_data/contract_request.json | envsubst)"

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
echo "Agreement achieved: $agreement_id" >&2

################################ Step 6.4: Using client extension, get data from agreement directly as response to check if data transfer works ################################
pull_request_dto="$(to_compose_url < system-tests/resources/request_data/pull_request_dto.json | envsubst)"

edr=$(curl \
  --silent \
  --show-error \
  --request POST \
  --url "http://localhost:23337/api/v1/dspclient/negotiate" \
  --data "$pull_request_dto" \
  --header "x-api-key: password" \
  --header "Content-Type: application/json")

printf '%s\n' "$edr" > logs/edr.log

################################ Step 6.5: Check the received EDR against system-tests/resources/edr.json ################################
endpoint=$(printf '%s\n' "$edr" | jq -r '.properties["https://w3id.org/edc/v0.0.1/ns/endpoint"]')
authorization=$(printf '%s\n' "$edr" | jq -r '.properties["https://w3id.org/edc/v0.0.1/ns/authorization"]')

edr_met=$(jq -n \
  --argjson expected "$(jq -cS '.properties' system-tests/resources/edr.json)" \
  --argjson received "$(printf '%s\n' "$edr" | jq -cS '.properties')" \
  '($expected | to_entries) | all(. as $entry | ($received[$entry.key] != null and $received[$entry.key] == $entry.value))')

if [[ "${edr_met}" != "true" ]]; then
  echo "ERR: Received EDR does not match system-tests/resources/edr.json" >&2
  diff <(printf '%s\n' "$edr" | jq -S '.properties') <(jq -S '.properties' system-tests/resources/edr.json) >&2 || true
  exit 1
fi
echo "EDR matches edr.json."

################################ Step 6.6: GET the actual data through the EDR endpoint ################################
response=$(curl \
  --silent \
  --show-error \
  --request GET \
  --url "${endpoint}" \
  --header "Authorization: ${authorization}")

printf '%s\n' "$response" | tee logs/received_data.log

received_data=$(printf '%s\n' "$response" | jq -S -r '.')

should_be_data=$(< system-tests/resources/aas.json jq -S -r '.conceptDescriptions[0]')

diff -w <(echo "$received_data") <(echo "$should_be_data") > /dev/null

echo "Transferred data checks out. Test complete!"
