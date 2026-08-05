#!/usr/bin/env bash
source /scripts/util.sh

printf "Initializing issuer...\n"

adminApiKey="$(jq -r '.apiKey' /scripts/data/issuer.json)"
issuanceApi="$(jq -r '.issuanceApi' /scripts/data/issuer.json)"
identityApi="$(jq -r '.identityApi' /scripts/data/issuer.json)"
issuerApi="$(jq -r '.issuerApi' /scripts/data/issuer.json)"
did="$(jq -r '.did' /scripts/data/issuer.json)"
b64=$(printf %s "$did" | base64 | tr -d '\n')
contextId="$(jq -r '.contextId' /scripts/data/issuer.json)"

###########################
## Issuer Service ##
###########################
printf "Creating issuer participant\n"
# Issuer Participant
issuer_participant="$(
  jq \
  --arg url "$issuanceApi" \
  --arg did "$did" \
  --arg contextId "$contextId" \
  --arg b64 "$b64" \
  --arg serviceType "IssuerService" \
  '
    .serviceEndpoints[0].type = $serviceType
    | .serviceEndpoints[0].serviceEndpoint = ($url + "/v1alpha/participants/" + $b64)
    | .participantId = $did
    | .participantContextId = $contextId
    | .did = $did
    | .key.keyId = ($did + "#key-1")
    | .key.privateKeyAlias = $did
  ' /scripts/templates/participant.json
)"

response=$(post_json "$identityApi/v1alpha/participants" "$adminApiKey" "$issuer_participant")

issuerApiKey=$(echo "$response"| jq -r '.apiKey')
issuerClientId=$(echo "$response"| jq -r '.clientId')
issuerClientSecret=$(echo "$response"| jq -r '.clientSecret')

echo "Client API Key ($did): $issuerApiKey"
echo "Client ID ($did): $issuerClientId"
echo "Client Secret ($did): $issuerClientSecret"

printf "Creating Attestation"
post_json "$issuerApi/v1alpha/participants/$b64/attestations" "$adminApiKey" "$(jq '.'  /scripts/templates/attestation.json)"

printf "Creating MembershipCredential"
post_json "$issuerApi/v1alpha/participants/$b64/credentialdefinitions" "$adminApiKey" "$(jq '.'  /scripts/templates/credential.json)"

printf "\n--- Initialized issuer ---"
