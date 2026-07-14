#!/usr/bin/env bash

function initialize() {
    participantFile=$1

    vaultUrl=http://vault:${VAULT_PORT:-8200}/v1/secret/data
    vaultToken=${VAULT_TOKEN:-token}

    issuerAdminApiKey="$(jq -r '.apiKey' /scripts/data/issuer.json)"
    issuerIssuerApi="$(jq -r '.issuerApi' /scripts/data/issuer.json)"
    issuerDid="$(jq -r '.did' /scripts/data/issuer.json)"

    did="$(jq -r '.did' "$participantFile")"
    contextId="$(jq -r '.contextId' "$participantFile")"
    adminApiKey="$(jq -r '.apiKey' "$participantFile")"
    credentialsApi="$(jq -r '.credentialsApi' "$participantFile")"
    identityApi="$(jq -r '.identityApi' "$participantFile")"
    stsClientSecretAlias="$(jq -r '.stsClientSecretAlias' "$participantFile")"

    secret=$(create_participant "$did" "$adminApiKey" "$credentialsApi" "$identityApi" "$contextId")
    store_secret "$vaultUrl" "$vaultToken" "$stsClientSecretAlias" "$secret"
    create_holder_at_issuer "$did" "$issuerDid" "$issuerAdminApiKey" "$issuerIssuerApi"
    request_verifiable_credential "$did" "$adminApiKey" "$identityApi" "$issuerDid"
}

function create_participant() {
  did=$1
  adminApiKey=$2
  credentialsApi=$3
  identityApi=$4
  contextId=$5
  b64=$(printf %s "$did" | base64 | tr -d '\n')

  participant="$(
    jq \
    --arg credentialsApi "$credentialsApi" \
    --arg contextId "$contextId" \
    --arg did "$did" \
    --arg b64 "$b64" \
    --arg serviceType "CredentialService" \
    '
      .serviceEndpoints[0].type = $serviceType
      | .serviceEndpoints[0].serviceEndpoint = ($credentialsApi + "/v1/participants/" + $b64)
      | .participantId = $did
      | .participantContextId = $contextId
      | .did = $did
      | .key.keyId = ($did + "#key-1")
      | .key.privateKeyAlias = $did
    ' /scripts/templates/participant.json
  )"

  printf "\nCreating participant for %s" "$did"
  response=$(curl -fsSL\
    --request POST \
    --url "$identityApi/v1alpha/participants" \
    --header 'content-type: application/json' \
    --header "x-api-key: $adminApiKey" \
    --data "$participant")

  participantApiKey=$(echo "$response"| jq -r '.apiKey')
  participantClientId=$(echo "$response"| jq -r '.clientId')
  participantClientSecret=$(echo "$response"| jq -r '.clientSecret')

  echo "Client API Key ($did): $participantApiKey"
  echo "Client ID ($did): $participantClientId"
  echo "Client Secret ($did): $participantClientSecret"

  printf "%s" "$participantClientSecret"
}

function create_holder_at_issuer() {
  did=$1
  issuerDid=$2
  issuerAdminApiKey=$3
  issuerIssuerApi=$4
  issuerB64=$(printf %s "$issuerDid" | base64 | tr -d '\n')

  printf "\nCreating holder for participant %s at issuer %s" "$did" "$issuerDid"
  holder="$(jq --arg did "$did" '.holderId = $did | .did = $did | .name = $did' /scripts/templates/holder.json)"
  curl -fsSL\
    --url "$issuerIssuerApi/v1alpha/participants/$issuerB64/holders" \
    --header 'content-type: application/json' \
    --header "x-api-key: $issuerAdminApiKey" \
    --data "$holder"
}

function store_secret() {
  vaultUrl=$1
  vaultToken=$2
  alias=$3
  secretValue=$4

  printf "\nStoring secret %s at vault %s" "$alias" "$vaultUrl"
  curl --url "$vaultUrl/$alias" \
    --header "X-Vault-Token: $vaultToken" \
    --header "Content-Type: application/json" \
    --data "{\"data\":{\"content\":\"$secretValue\"}}"
}

function request_verifiable_credential() {
  did=$1
  adminApiKey=$2
  identityApi=$3
  issuerDid=$4
  b64=$(printf %s "$did" | base64 | tr -d '\n')

  cred_req="$(jq --arg did "$issuerDid" ' .issuerDid = $did ' /scripts/templates/credential_request.json)"

  printf "\nRequesting verifiable credential for participant %s from issuer %s" "$did" "$issuerDid"
  curl -fsSL \
    --url "$identityApi/v1alpha/participants/$b64/credentials/request" \
    --header 'content-type: application/json' \
    --header "x-api-key: $adminApiKey" \
    --data "$cred_req"
}

printf "\n\nInitializing participants...\n"
initialize "/scripts/data/participant1.json"
initialize "/scripts/data/participant2.json"
