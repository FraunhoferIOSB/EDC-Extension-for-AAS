# DSP Client Extension

This extension automates contract negotiation and data transfer for a consumer. For this, acceptable contracts need to
be defined (alternatively, all provider contracts can be accepted).

## Configuration

The common key for configurations is `edc.client.dsp.`

| Key                 | Value Type              | Description                                                                      |
|:--------------------|:------------------------|:---------------------------------------------------------------------------------|
| policy.accepted.all | boolean                 | Whether all policies should be accepted. Default is false                        |
| timeout.catalog     | whole number in seconds | How long should the client wait for a catalog? Default value is 20(s).           |
| timeout.negotiation | whole number in seconds | How long should the client wait for a negotiation? Default value is 20(s).       |
| timeout.transfer    | whole number in seconds | How long should the client wait for a transfer to start? Default value is 20(s). |

## Interfaces

The client extension publishes all of its API over the EDC default api endpoint. Thus, all endpoints start with
`/api/v1/dspclient/`. The authentication configured on the default api context applies to all endpoints: with
`tokenbased` authentication, requests without a valid `x-api-key` header are rejected with `401`.

### Negotiate

`POST negotiate` with a `PullRequestDTO` body: fetch the provider's catalog, check for an acceptable policy, negotiate
a contract, and — unless `starttransfer=false` — start a PULL transfer (type `ProxyHttpData` by default) and return the
resulting EDR `DataAddress`.

The `starttransfer` query parameter binds case-sensitively: only the lowercase form is honored. `?startTransfer=false`
is silently ignored, meaning the transfer is started anyway.

| Status | Condition                                                                                                                               | Response body |
|:-------|:----------------------------------------------------------------------------------------------------------------------------------------|:--------------|
| 200    | Contract negotiated; with `starttransfer=false` no transfer is started                                                                 | `starttransfer=true` or omitted: the EDR `DataAddress` (its type lives under `properties."https://w3id.org/edc/v0.0.1/ns/type"`); `starttransfer=false`: the negotiated `ContractAgreement` |
| 400    | No acceptable policy found (`policy.accepted.all=false`), catalog fetch failed (including provider errors), negotiation terminated or timed out, or the request body misses a required `PullRequestDTO` field | Catalog/selection failures: `ApiErrorDetail` JSON; negotiation failures and DTO binding errors: plain-text detail |
| 401    | Missing or invalid `x-api-key` (if the default api context requires authentication)                                                      | - |
| 404    | Asset is not contained in the provider's catalog                                                                                        | `ApiErrorDetail` JSON |
| 409    | Catalog conflict, e.g. duplicate matching datasets                                                                                      | `ApiErrorDetail` JSON |
| 422    | The transfer was terminated before data-address information was available                                                                | Plain-text failure detail |
| 500    | Unexpected errors, e.g. the transfer did not start within `timeout.transfer`, or the provider returned a non-JSON catalog               | No error detail entity |

### Accepted policy definitions

CRUD for the policy definitions checked during negotiation when `policy.accepted.all=false`.

| HTTP Method | Endpoint                                          | Request Body     | Description                                                                                                                                    |
|:------------|:--------------------------------------------------|:-----------------|:-----------------------------------------------------------------------------------------------------------------------------------------------|
| GET         | policydefinitions/accepted                        | -                | Returns the accepted policy definitions (`200`, empty store: `[]`).                                                                            |
| POST        | policydefinitions/accepted                        | PolicyDefinition | Adds the given policy definition (`204`). A bare policy is stored only once; a policy bearing rules can be stored twice (deduplication relies on identity-based policy-rule equality). |
| PUT         | policydefinitions/accepted                        | PolicyDefinition | Creates (`201` with a `Location` header, absolutized against the api context root) or replaces (`202`, body is the id serialized as JSON string) the stored policy definition with the same id. |
| DELETE      | policydefinitions/accepted/{policyDefinitionId}   | -                | Deletes the accepted policy definition with the given id (`204`; unknown id: `404` with `ApiErrorDetail` JSON).                                 |

`POST` and `PUT` answer `400` for an empty body (JSON `ApiErrorDetail`) or malformed JSON (plain text).

## DTOs

### PullRequestDTO

Fields:

- counterPartyAddress: string
- counterPartyId: string
- assetId: string
- participantContextId: string(optional)
- transferType: string(optional, default="ProxyHttpData")

### Dependencies

| Name                                       | Description                   |
|:-------------------------------------------|:------------------------------|
| org.eclipse.edc:web-spi                    | REST Controllers              |
| org.eclipse.edc:control-plane-contract     | Observe contract negotiations |
| org.eclipse.edc:control-plane-transfer     | Observe transfer processes    |
| org.eclipse.edc:dsp-catalog-transform-2025 | JSON to Catalog transformers |
| org.eclipse.edc:json-ld-lib                | JsonLD expansion              |
