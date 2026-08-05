
post_json() {
  local url="$1"
  local api_key="$2"
  local data="$3"

  local response
  local http_code
  local body

  response=$(curl -sS \
    -X POST \
    --url "$url" \
    --header 'content-type: application/json' \
    --header "x-api-key: $api_key" \
    --data "$data" \
    --write-out "\n%{http_code}")

  http_code=$(echo "$response" | tail -n1)
  body=$(echo "$response" | sed '$d')

  if [[ ! "$http_code" =~ ^2 ]]; then
    echo "Request failed with HTTP $http_code" >&2
    echo "curl $url \\" >&2
    echo "--header 'content-type: application/json'"\\
    echo "--header 'x-api-key: $api_key'"\\
    echo "-d '" >&2
    echo "$data'" >&2
    echo "Error response:" >&2
    echo "$body" >&2
  fi

  echo "$body"
}