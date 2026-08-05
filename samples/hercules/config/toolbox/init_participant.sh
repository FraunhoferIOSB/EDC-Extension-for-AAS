#!/usr/bin/env bash
source /scripts/util.sh

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
    dataPlanePrivateKeyAlias="$(jq -r '.dataPlanePrivateKeyAlias' "$participantFile")"
    dataPlanePublicKeyAlias="$(jq -r '.dataPlanePublicKeyAlias' "$participantFile")"

    secret=$(create_participant "$did" "$adminApiKey" "$credentialsApi" "$identityApi" "$contextId")
    store_secret "$vaultUrl" "$vaultToken" "$stsClientSecretAlias" "$secret"
    create_holder_at_issuer "$did" "$issuerDid" "$issuerAdminApiKey" "$issuerIssuerApi"
    request_verifiable_credential "$did" "$adminApiKey" "$identityApi" "$issuerDid" "$contextId"
    initialize_data_plane "$vaultUrl" "$vaultToken" "$dataPlanePrivateKeyAlias" "$dataPlanePublicKeyAlias"
}

function create_participant() {
  did=$1
  adminApiKey=$2
  credentialsApi=$3
  identityApi=$4
  contextId=$5
  b64=$(printf %s "$did" | base64 | tr -d '\n')
  contextIdB64=$(printf %s "$contextId" | base64 | tr -d '\n')

  participant="$(
    jq \
    --arg credentialsApi "$credentialsApi" \
    --arg contextId "$contextId" \
    --arg did "$did" \
    --arg contextIdB64 "$contextIdB64" \
    --arg serviceType "CredentialService" \
    '
      .serviceEndpoints[0].type = $serviceType
      | .serviceEndpoints[0].serviceEndpoint = ($credentialsApi + "/v1/participants/" + $contextIdB64)
      | .participantId = $did
      | .participantContextId = $contextId
      | .did = $did
      | .key.keyId = ($did + "#key-1")
      | .key.privateKeyAlias = $did
    ' /scripts/templates/participant.json
  )"

  printf "\nCreating participant for %s\n" "$did" >&2
  response=$(post_json "$identityApi/v1alpha/participants" "$adminApiKey" "$participant")

  participantApiKey=$(echo "$response"| jq -r '.apiKey')
  participantClientId=$(echo "$response"| jq -r '.clientId')
  participantClientSecret=$(echo "$response"| jq -r '.clientSecret')

  echo "Client API Key ($did): $participantApiKey" >&2
  echo "Client ID ($did): $participantClientId" >&2
  echo "Client Secret ($did): $participantClientSecret" >&2

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
  post_json "$issuerIssuerApi/v1alpha/participants/$issuerB64/holders" "$issuerAdminApiKey" "$holder"
}

function store_secret() {
  vaultUrl=$1
  vaultToken=$2
  alias=$3
  secretValue=$4

  printf "\nStoring secret %s at vault %s" "$alias" "$vaultUrl" >&2

  payload="$(jq -n --arg secretValue "$secretValue" \
    '{data: {content: $secretValue}}'
  )"

  curl -fsSL \
    --url "$vaultUrl/$alias" \
    --header "X-Vault-Token: $vaultToken" \
    --header "Content-Type: application/json" \
    --data "$payload"
}

function request_verifiable_credential() {
  did=$1
  adminApiKey=$2
  identityApi=$3
  issuerDid=$4
  contextId=$5
  contextIdB64=$(printf %s "$contextId" | base64 | tr -d '\n')

  cred_req="$(jq --arg did "$issuerDid" ' .issuerDid = $did ' /scripts/templates/credential_request.json)"

  printf "\nRequesting verifiable credential for participant %s from issuer %s" "$did" "$issuerDid"
  post_json "$identityApi/v1alpha/participants/$contextIdB64/credentials/request" "$adminApiKey" "$cred_req"
}

function initialize_data_plane() {
  vaultUrl=$1
  vaultToken=$2
  privKeyAlias=$3
  pubKeyAlias=$4

  printf "\nGenerating keypair for dataplane to create EDRs"
  # create rsa keypair
  openssl genrsa -out /tmp/priv_pkcs1.pem 2048
  openssl pkcs8 -topk8 -nocrypt -in /tmp/priv_pkcs1.pem -out /tmp/priv_pkcs8.pem
  openssl rsa -in /tmp/priv_pkcs1.pem -pubout -out /tmp/pub.pem
  
  privKey="$(cat /tmp/priv_pkcs8.pem)"
  pubKey="$(cat /tmp/pub.pem)"
  
  # deploy secrets for provider dataplane to vault
  store_secret "$vaultUrl" "$vaultToken" "$privKeyAlias" "$privKey"

  store_secret "$vaultUrl" "$vaultToken" "$pubKeyAlias" "$pubKey"
}

printf "\n\nInitializing participants...\n"
initialize "/scripts/data/participant1.json"
initialize "/scripts/data/participant2.json"
