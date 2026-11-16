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
`/api/v1/dspclient/`

| HTTP Method | Endpoint                   | Parameters ((r) = required)                         | Request Body     | Description                                                                                                |
|:------------|:---------------------------|:----------------------------------------------------|:-----------------|:-----------------------------------------------------------------------------------------------------------|
| POST        | negotiate                  | QueryParam "startTransfer": boolean (default: true) | PullRequestDTO   | Negotiate with provider over asset. If startTransfer=true, also start transfer process (HttpData-PULL)     |
| GET         | policydefinitions/accepted | -                                                   | -                | Returns the client extension's accepted policy definitions.                                                |
| POST        | policydefinitions/accepted | -                                                   | PolicyDefinition | Adds the given PolicyDefinition to the accepted PolicyDefinitions list                                     |
| PUT         | policydefinitions/accepted | -                                                   | PolicyDefinition | Deletes a client extension's accepted policy definition with the same policyDefinitionId as the request.   |
| DELETE      | policydefinitions/accepted | PathParam PolicyDefinitionId (r)                    | -                | Updates the client extension's accepted policy definition with the same policyDefinitionId as the request. |

## DTOs

### PullRequestDTO

Fields:

- counterPartyAddress: string
- counterPartyId: string
- assetId: string
- participantContextId: string(optional)

### Dependencies

| Name                                       | Description                   |
|:-------------------------------------------|:------------------------------|
| org.eclipse.edc:web-spi                    | REST Controllers              |
| org.eclipse.edc:control-plane-contract     | Observe contract negotiations |
| org.eclipse.edc:control-plane-transform    | Observe transfer processes    |
| org.eclipse.edc:dsp-catalog-transform-2025 | EDC constants                 |
| org.eclipse.edc:json-ld-lib                | JsonLD expansion              |
