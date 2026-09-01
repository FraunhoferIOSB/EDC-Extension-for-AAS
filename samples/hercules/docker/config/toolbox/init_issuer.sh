#!/usr/bin/env bash
source /scripts/util.sh

printf "Initializing issuer...\n"

adminApiKey="$(jq -r '.apiKey' /scripts/data/issuer.json)"
issuanceApi="$(jq -r '.issuanceApi' /scripts/data/issuer.json)"
identityApi="$(jq -r '.identityApi' /scripts/data/issuer.json)"
issuerApi="$(jq -r '.issuerApi' /scripts/data/issuer.json)"
did="$(jq -r '.did' /scripts/data/issuer.json)"
contextId="$(jq -r '.contextId' /scripts/data/issuer.json)"
contextIdB64=$(printf %s "$contextId" | base64 | tr -d '\n')
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
  --arg contextIdB64 "$contextIdB64" \
  --arg serviceType "IssuerService" \
  '
    .serviceEndpoints[0].type = $serviceType
    | .serviceEndpoints[0].serviceEndpoint = ($url + "/participants/" + $contextId)
    | .participantId = $did
    | .participantContextId = $contextId
    | .did = $did
    | .key.keyId = ($did + "#key-1")
    | .key.privateKeyAlias = $did
  ' /scripts/templates/participant.json
)"

response=$(post_json "$identityApi/participants" "$adminApiKey" "$issuer_participant")

issuerApiKey=$(echo "$response"| jq -r '.apiKey')
issuerClientId=$(echo "$response"| jq -r '.clientId')
issuerClientSecret=$(echo "$response"| jq -r '.clientSecret')

echo "Client API Key ($did): $issuerApiKey"
echo "Client ID ($did): $issuerClientId"
echo "Client Secret ($did): $issuerClientSecret"

printf "Creating Attestation"
post_json "$issuerApi/participants/$contextId/attestations" "$adminApiKey" "$(jq '.'  /scripts/templates/attestation.json)"

printf "Creating MembershipCredential"
post_json "$issuerApi/participants/$contextId/credentialdefinitions" "$adminApiKey" "$(jq '.'  /scripts/templates/credential.json)"

printf "Initialized issuer"
