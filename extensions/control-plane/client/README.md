# Client Extension

This extension automates contract negotiation and data transfer for a consumer. For this, acceptable contracts need to
be defined (alternatively, all provider contracts can be accepted).

## Configuration

| Key (edc.client.)             | Value Type              | Description                                                                                                                                                                   |
|:------------------------------|:------------------------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| acceptAllProviderOffers       | boolean                 | Accept any contractOffer offered by all provider connectors on automated contract negotiation (e.g., trusted provider)                                                        |
| acceptedPolicyDefinitionsPath | path                    | Path pointing to a JSON-file containing acceptable PolicyDefinitions for automated contract negotiation in a list (only policies must match in a provider's PolicyDefinition) |
| waitForAgreementTimeout       | whole number in seconds | How long should the extension wait for an agreement when automatically negotiating a contract? Default value is 20(s).                                                        |
| waitForCatalogTimeout         | whole number in seconds | How long should the extension wait for a catalog? Default value is 20(s).                                                                                                     |
| waitForTransferTimeout        | whole number in seconds | How long should the extension wait for a data transfer when automatically negotiating a contract? Default value is 20(s).                                                     |

## Interfaces

The client extension publishes all of its API over the EDC api endpoint that can be configured by `web.http.port`,
`web.http.path`, `web.http.default.auth.type`, `web.http.default.auth.key.alias` and/or `web.http.default.auth.key`. All
endpoints are authenticated if the respective EDC endpoint requires authentication.

| HTTP Method | Interface (/api/automated/...) | Query Parameters ((r) = required)                                                                  | Description                                                                                                                                                                                                                                                                                                                                        |
|:------------|:-------------------------------|:---------------------------------------------------------------------------------------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| POST        | negotiate                      | "providerUrl": URL (r), "providerId": String (r), "assetId": String (r), "dataDestinationUrl": URL | Perform an automated contract negotiation with a provider (given provider URL and ID) and get the data stored for the specified asset. Optionally, a data destination URL can be specified where the data is sent to instead of the extension's log, or a data address can be provided through the request body which defines the data destination |
| GET         | dataset                        | "providerUrl": URL (r), "assetId": String (r), "providerId": String (r)                            | Get dataset from the specified provider's catalog that contains the specified asset's policies.                                                                                                                                                                                                                                                    |
| POST        | negotiateContract              | Request Body: org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest (r)         | Using a contractRequest (JSON in http request body), negotiate a contract. Returns the corresponding agreementId on success.                                                                                                                                                                                                                       |
| GET         | transfer                       | "providerUrl": URL (r), "agreementId": String (r), "assetId": String (r), "dataDestinationUrl"     | Submits a data transfer request to the providerUrl. On success, returns the data behind the specified asset. Optionally, a data destination URL can be specified where the data is sent to instead of the extension's log.                                                                                                                         |
| POST        | acceptedPolicies               | request body: List of PolicyDefinitions (JSON) (r)                                                 | Adds the given PolicyDefinitions to the accepted PolicyDefinitions list (Explanation: On fully automated negotiation, the provider's PolicyDefinition is matched against the consumer's accepted PolicyDefinitions list. If any PolicyDefinition fits the provider's, the negotiation continues.) Returns "OK"-Response if requestBody is valid.   |
| GET         | acceptedPolicies               | -                                                                                                  | Returns the client extension's accepted policy definitions for fully automated negotiation.                                                                                                                                                                                                                                                        |
| DELETE      | acceptedPolicies               | request body: PolicyDefinition: PolicyDefinition (JSON) (r)                                        | Updates the client extension's accepted policy definition with the same policyDefinitionId as the request.                                                                                                                                                                                                                                         |
| PUT         | acceptedPolicies               | request body: PolicyDefinitionId: String (JSON) (r)                                                | Deletes a client extension's accepted policy definition with the same policyDefinitionId as the request.                                                                                                                                                                                                                                           |

### Dependencies

| Name                                        | Description                                    |
|:--------------------------------------------|:-----------------------------------------------|
| public-api-management (local)               | Centralized http authentication request filter |
| org.eclipse.edc:connector-core              | PolicyService                                  |
| org.eclipse.edc:control-plane-contract      | Observe contract negotiations                  |
| org.eclipse.edc:control-plane-transform     | Type transformers                              |
| org.eclipse.edc:data-plane-http-spi         | HttpDataAddress                                |
| org.eclipse.edc:dsp-catalog-http-dispatcher | EDC constants                                  |
| org.eclipse.edc:federated-catalog-core      | Transformers                                   |
| org.eclipse.edc:federated-catalog-core2025  | JsonObjectToCatalogTransformer                 |
| org.eclipse.edc:json-ld-lib                 | JsonLD expansion                               |
