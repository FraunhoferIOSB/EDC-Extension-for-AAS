# EDC Extension for Asset Administration Shell [![Build Status](https://github.com/FraunhoferIOSB/EDC-Extension-for-AAS/actions/workflows/gradle.yml/badge.svg)](https://github.com/FraunhoferIOSB/EDC-Extension-for-AAS/actions)

This [Eclipse Dataspace Connector (EDC)](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector) extension
provides an easy way to share
an [Asset Administration Shell (AAS)](https://www.plattform-i40.de/SiteGlobals/IP/Forms/Listen/Downloads/EN/Downloads_Formular.html?cl2Categories_TechnologieAnwendungsbereich_name=Verwaltungsschale)
model via the EDC.

## Version compatibility

| Specification                                                                                                                                                                                                                                                                | Version                                                                                                      |
|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| [Eclipse Dataspace Connector](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector)                                                                                                                                                                              | v0.6.4                                                                                                       |
| [AAS - Details of the Asset Administration Shell - Part 1](https://www.plattform-i40.de/IP/Redaktion/EN/Downloads/Publikation/Details_of_the_Asset_Administration_Shell_Part1_V3.html)<br />The exchange of information between partners in the value chain of Industrie 4.0 | AAS Specs – Part 1 V3.0 (final)<br/>(based on [eclipse-aas4j/aas4j](https://github.com/eclipse-aas4j/aas4j)) |

## Repo Structure

The repository contains several material:

- `client`: Source code for the client extension (automated contract negotiation)
- `config`: Checkstyle files for code formatting
- `edc-extension4aas`: Source code for the AAS extension
- `example`: Example use case for the edc-extension4aas and client extension with a preconfigured EDC launcher.
- `public-api-management`: Small extension for managing outward facing endpoints which require no authentication

<!-- ------------------Template Section --------------------------- -->

## Example Usage

For a data transfer example using two connectors communicating with the DSP protocol, check
the [Example's README](example/README.md).

## Functionality

AAS data can be shared over the EDC by linking an EDC Asset to the HTTP endpoint of the AAS element. Additionally,
contracts have to be defined for each element. In order to minimize configuration effort and prevent errors, this
extension is able to link running AAS into EDC Assets. Furthermore, this extension can also start AAS by reading an AAS
model. A default contract can be chosen to be applied for all elements. For critical elements, additional contracts can
be placed. External changes to the model of an AAS are automatically synchronized by the extension.

Additionally, a client extension providing API calls for aggregations of processes such as contract negotiation and data
transfer is available. The result is a one-click negotiation and data transfer, ideal for SME or individuals.

### Use Cases

Provide digital twin (AAS) data to business partners in Data Spaces like Catena-X or Manufacturing-X.

## Technical Details

### Interfaces

<details>

<summary>Provider Interfaces</summary>

| HTTP Method | Interface (edc:1234/api/...) ((a) = only for authenticated users) | Parameters ((r) = required)                                                                                                                                                              | Description                                                                                                                                                                                                                                                                                                              |
|:------------|:------------------------------------------------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| GET         | config (a)                                                        | -                                                                                                                                                                                        | Get current extension configuration values.                                                                                                                                                                                                                                                                              |
| PUT         | config (a)                                                        | Body: Updated config values (JSON) (r)                                                                                                                                                   | Update config values.                                                                                                                                                                                                                                                                                                    |
| POST        | client (a)                                                        | Query Parameter "url" (r)                                                                                                                                                                | Register a standalone AAS service (e.g., FA³ST) to this extension.                                                                                                                                                                                                                                                       |
| DELETE      | client (a)                                                        | Query Parameter "url" (r)                                                                                                                                                                | Unregister an AAS service (e.g., FA³ST) from this extension, possibly shutting down the service if it has been started internally.                                                                                                                                                                                       |
| POST        | environment (a)                                                   | Query Parameter "environment": Path to new AAS environment (r), Query Parameter "port": Port of service to be created , Query Parameter "config": Path of AAS service configuration file | Create a new AAS service. Either (http) "port" or "config" must be given to ensure communication with the AAS service via an HTTP endpoint on the service's side. This command returns the URL of the newly created AAS service on success, which can be used to remove the service using the interface "DELETE /client" |
| POST        | aas (a)                                                           | Query Parameter "requestUrl": URL of AAS service to be updated (r), request body: AAS element (r)                                                                                        | Forward POST request to provided host in requestUrl. If requestUrl is an AAS service that is registered at this EDC, synchronize assets and self description as well.                                                                                                                                                    |
| DELETE      | aas (a)                                                           | Query Parameter requestUrl: URL of AAS service to be updated (r)                                                                                                                         | Forward DELETE request to provided host in requestUrl. If requestUrl is an AAS service that is registered at this EDC, synchronize assets and self description as well.                                                                                                                                                  |
| PUT         | aas (a)                                                           | Query Parameter "requestUrl": URL of AAS service to be updated (r), request body: AAS element (r)                                                                                        | Forward PUT request to provided host in requestUrl.                                                                                                                                                                                                                                                                      |
| GET         | selfDescription                                                   | -                                                                                                                                                                                        | Return self description of extension.                                                                                                                                                                                                                                                                                    |

</details>

<details>
<summary>Client Interfaces</summary>

| HTTP Method | Interface (edc:1234/api/automated/...) ((a) = only for authenticated users) | Parameters ((r) = required)                                                                                                                                        | Description                                                                                                                                                                                                                                                                                                                                      |
|:------------|:----------------------------------------------------------------------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| POST        | negotiate (a)                                                               | Query Parameter "providerUrl": URL (r), Query Parameter "providerId": String (r), Query Parameter "assetId": String (r), Query Parameter "dataDestinationUrl": URL | Perform an automated contract negotiation with a provider (given provider URL and ID) and get the data stored for the specified asset. Optionally, a data destination URL can be specified where the data is sent to instead of the extension's log.                                                                                             |
| GET         | dataset (a)                                                                 | Query Parameter "providerUrl": URL (r), Query Parameter "assetId": String (r), Query Parameter "providerId": String (r)                                            | Get dataset from the specified provider's catalog that contains the specified asset's policies.                                                                                                                                                                                                                                                  |
| POST        | negotiateContract (a)                                                       | request body: org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest (r)                                                                         | Using a contractRequest (JSON in http request body), negotiate a contract. Returns the corresponding agreementId on success.                                                                                                                                                                                                                     |
| GET         | transfer (a)                                                                | Query Parameter "providerUrl": URL (r), Query Parameter "agreementId": String (r), Query Parameter "assetId": String (r), Query Parameter "dataDestinationUrl"     | Submits a data transfer request to the providerUrl. On success, returns the data behind the specified asset. Optionally, a data destination URL can be specified where the data is sent to instead of the extension's log.                                                                                                                       |
| POST        | acceptedPolicies (a)                                                        | request body: List of PolicyDefinitions (JSON) (r)                                                                                                                 | Adds the given PolicyDefinitions to the accepted PolicyDefinitions list (Explanation: On fully automated negotiation, the provider's PolicyDefinition is matched against the consumer's accepted PolicyDefinitions list. If any PolicyDefinition fits the provider's, the negotiation continues.) Returns "OK"-Response if requestBody is valid. |
| GET         | acceptedPolicies (a)                                                        | -                                                                                                                                                                  | Returns the client extension's accepted policy definitions for fully automated negotiation.                                                                                                                                                                                                                                                      |
| DELETE      | acceptedPolicies (a)                                                        | request body: PolicyDefinition: PolicyDefinition (JSON) (r)                                                                                                        | Updates the client extension's accepted policy definition with the same policyDefinitionId as the request.                                                                                                                                                                                                                                       |
| PUT         | acceptedPolicies (a)                                                        | request body: PolicyDefinitionId: String (JSON) (r)                                                                                                                | Deletes a client extension's accepted policy definition with the same policyDefinitionId as the request.                                                                                                                                                                                                                                         |

</details>

### Dependencies

#### EDC-Extension-for-AAS

| Name                                                | Description                                                                                                                      |
|:----------------------------------------------------|:---------------------------------------------------------------------------------------------------------------------------------|
| public-api-management (local)                       | Centralized http authentication request filter                                                                                   |
| de.fraunhofer.iosb.ilt.faaast.service:starter       | [FA³ST Service](https://github.com/FraunhoferIOSB/FAAAST-Service) to start AAS services internally.                              |
| org.eclipse.digitaltwin.aas4j:aas4j-dataformat-json | [Eclipse AAS4J JSON (de-)serializer](https://github.com/eclipse-aas4j/aas4j/tree/main/dataformat-json) (de-)serialize AAS models |
| org.eclipse.digitaltwin.aas4j:aas4j-model           | [Eclipse AAS4J java model](https://github.com/eclipse-aas4j/aas4j/tree/main/model)                                               |
| org.eclipse.edc:data-plane-http                     | EDC HttpRequestFactory Field                                                                                                     |
| org.eclipse.edc:data-plane-http-spi                 | EDC HttpDataAddress Field                                                                                                        |
| org.eclipse.edc:data-plane-spi                      | EDC DataSource, StreamResult Field                                                                                               |
| org.eclipse.edc:management-api                      | EDC Asset/Contract Management                                                                                                    |

#### Client Extension

| Name                                        | Description                                    |
|:--------------------------------------------|:-----------------------------------------------|
| public-api-management (local)               | Centralized http authentication request filter |
| org.eclipse.edc:connector-core              | PolicyService                                  |
| org.eclipse.edc:control-plane-contract      | Observe contract negotiations                  |
| org.eclipse.edc:control-plane-transform     | Type transformers                              |
| org.eclipse.edc:data-plane-http-spi         | HttpDataAddress                                |
| org.eclipse.edc:dsp-catalog-http-dispatcher | EDC constants                                  |
| org.eclipse.edc:json-ld-lib                 | JsonLD expansion                               |
| org.eclipse.edc:management-api              | EDC WebService for registering endpoints       |

#### Public API Management Extension

| Name                     | Description            |
|:-------------------------|:-----------------------|
| org.eclipse.edc:auth-spi | EDC Authentication SPI |

### Configurations

<details>

<summary>EDC-Extension-for-AAS Configurations</summary>

| Key                                  | Value Type                | Description                                                                                                                                                                                                                                             |
|:-------------------------------------|:--------------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| edc.aas.remoteAasLocation            | URL                       | A URL of an AAS service (such as FA³ST) that is already running and is conformant with official AAS API specification                                                                                                                                   |
| edc.aas.localAASModelPath            | path                      | A path to a serialized AAS environment compatible to specification version 3.0RC01 (see: https://github.com/FraunhoferIOSB/FAAAST-Service/blob/main/README.md)                                                                                          |
| edc.aas.localAASServicePort          | Open port from 1 to 65535 | Port to locally created AAS service. Required, if localAASModelPath is defined and localAASServiceConfigPath is not defined.                                                                                                                            |
| edc.aas.localAASServiceConfigPath    | path                      | Path to AAS config for locally started AAS service. Required, if localAASServicePort is not defined, but localAASModelPath is defined.                                                                                                                  |
| edc.aas.syncPeriod                   | whole number in seconds   | Time period in which AAS services should be polled for structural changes (added/deleted elements etc.). Default value is 5 (seconds). Note: This configuration value is only read on startup, the synchronization period cannot be changed at runtime. |
| edc.aas.exposeSelfDescription        | True/False                | Whether the Self Description should be exposed on {edc}/api/selfDescription. When set to False, the selfDescription is still available for authenticated requests.                                                                                      |
| edc.aas.defaultAccessPolicyPath      | path                      | Path to an access policy file (JSON). This policy will be used as the default access policy for all assets created after the configuration value has been set.                                                                                          |
| edc.aas.defaultContractPolicyPath    | path                      | Path to a contract policy file (JSON). This policy will be used as the default contract policy for all assets created after the configuration value has been set.                                                                                       |
| edc.aas.acceptSelfSignedCertificates | True/False                | Accept self-signed certificates from AAS services (internal+external)                                                                                                                                                                                   |
| edc.aas.onlySubmodels                | True/False                | (Provider) Only list submodels of AAS services                                                                                                                                                                                                          |

</details>

<details>

<summary>Client Extension Configurations</summary>  

| Key                                      | Value Type              | Description                                                                                                                                                                   |
|:-----------------------------------------|:------------------------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| edc.client.waitForAgreementTimeout       | whole number in seconds | How long should the extension wait for an agreement when automatically negotiating a contract? Default value is 10(s).                                                        |
| edc.client.waitForTransferTimeout        | whole number in seconds | How long should the extension wait for a data transfer when automatically negotiating a contract? Default value is 10(s).                                                     |
| edc.client.acceptAllProviderOffers       | boolean                 | If true, the client accepts any contractOffer offered by a provider connector on automated contract negotiation (e.g., trusted provider). Default value: false                |
| edc.client.acceptedPolicyDefinitionsPath | path                    | Path pointing to a JSON-file containing acceptable PolicyDefinitions for automated contract negotiation in a list (only policies must match in a provider's PolicyDefinition) |

</details>

## Terminology

| Term          | Description                                                                                                                                                                                                                                                                                                                      |
|:--------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| AAS           | Asset Administration Shell (see [AAS reading guide](https://www.plattform-i40.de/IP/Redaktion/DE/Downloads/Publikation/Asset_Administration_Shell_Reading_Guide.html) or [AAS specification part 1](https://www.plattform-i40.de/IP/Redaktion/DE/Downloads/Publikation/Details_of_the_Asset_Administration_Shell_Part1_V3.html)) |
| FA³ST Service | Open Source java implementation of the AAS part 2 [see on GitHub](https://github.com/FraunhoferIOSB/FAAAST-Service)                                                                                                                                                                                                              |

## Roadmap

Features in development:

- Graphical interface to simplify providing and requesting AAS (
  see: https://github.com/FraunhoferIOSB/EDC-Extension-for-AAS-Dashboard) &#x2713;
- AAS data-plane for EDC
- Docker Hub container deployment
