#!/usr/bin/env bash

printf "Initializing issuer...\n"
sleep 10

adminApiKey="$(jq -r '.apiKey' /scripts/data/issuer.json)"
issuanceApi="$(jq -r '.issuanceApi' /scripts/data/issuer.json)"
identityApi="$(jq -r '.identityApi' /scripts/data/issuer.json)"
issuerApi="$(jq -r '.issuerApi' /scripts/data/issuer.json)"
did="$(jq -r '.did' /scripts/data/issuer.json)"
b64=$(printf %s "$did" | base64 | tr -d '\n')

###########################
## Issuer Service ##
###########################
printf "Creating issuer participant\n"
# Issuer Participant
issuer_participant="$(
  jq \
  --arg url "$issuanceApi" \
  --arg did "$did" \
  --arg b64 "$b64" \
  --arg serviceType "IssuerService" \
  '
    .serviceEndpoints[0].type = $serviceType
    | .serviceEndpoints[0].serviceEndpoint = ($url + "/v1alpha/participants/" + $b64)
    | .participantId = $did
    | .did = $did
    | .key.keyId = ($did + "#key-1")
    | .key.privateKeyAlias = $did
  ' /scripts/templates/participant.json
)"

response=$(curl -fsSL\
  --url "$identityApi/v1alpha/participants" \
  --header 'content-type: application/json' \
  --header "x-api-key: $adminApiKey" \
  --data "$issuer_participant")

issuerApiKey=$(echo "$response"| jq -r '.apiKey')
issuerClientId=$(echo "$response"| jq -r '.clientId')
issuerClientSecret=$(echo "$response"| jq -r '.clientSecret')

echo "Client API Key ($did): $issuerApiKey"
echo "Client ID ($did): $issuerClientId"
echo "Client Secret ($did): $issuerClientSecret"

printf "\nCreating Attestation"
# Attestation 
curl -fsSL\
  --request POST \
  --url "$issuerApi/v1alpha/participants/$b64/attestations" \
  --header 'content-type: application/json' \
  --header "x-api-key: $adminApiKey" \
  --data "$(jq '.'  /scripts/templates/attestation.json)"

printf "\nCreating MembershipCredential"
# MembershipCredential 
curl -fsSL\
  --request POST \
  --url "$issuerApi/v1alpha/participants/$b64/credentialdefinitions" \
  --header 'content-type: application/json' \
  --header "x-api-key: $adminApiKey" \
  --data "$(jq '.'  /scripts/templates/credential.json)"

printf "\nInitialized issuer"
