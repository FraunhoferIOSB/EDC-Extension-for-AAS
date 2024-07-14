# EDC Extension for Asset Administration Shell [![Build Status](https://github.com/FraunhoferIOSB/EDC-Extension-for-AAS/actions/workflows/gradle.yml/badge.svg)](https://github.com/FraunhoferIOSB/EDC-Extension-for-AAS/actions)

This [Eclipse Dataspace Connector (EDC)](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector) extension
provides an easy way to share
an [Asset Administration Shell (AAS)](https://www.plattform-i40.de/SiteGlobals/IP/Forms/Listen/Downloads/EN/Downloads_Formular.html?cl2Categories_TechnologieAnwendungsbereich_name=Verwaltungsschale)
model via the EDC.

## Version compatibility

| Specification                                                                                                                                                                                                                                                                | Version                                                                                                      |
|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| [Eclipse Dataspace Connector](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector)                                                                                                                                                                              | v0.8.0                                                                                                       |
| [AAS - Details of the Asset Administration Shell - Part 1](https://www.plattform-i40.de/IP/Redaktion/EN/Downloads/Publikation/Details_of_the_Asset_Administration_Shell_Part1_V3.html)<br />The exchange of information between partners in the value chain of Industrie 4.0 | AAS Specs – Part 1 V3.0 (final)<br/>(based on [eclipse-aas4j/aas4j](https://github.com/eclipse-aas4j/aas4j)) |

## Repo Structure

The repository contains several material:

- `client`: The client extension: Automated contract negotiation from HTTP endpoints
- `config`: Checkstyle files for code formatting
- `data-plane-aas`: AAS data plane implementation (following HTTP data plane)
- `edc-extension4aas`: The AAS extension
- `example`: Example use case for the AAS extension with a preconfigured EDC launcher.
- `public-api-management`: Managing outward facing endpoints (http) which require no authentication

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

| HTTP Method | Interface (edc:1234/api/...) ((a) = only for authenticated users) | Parameters ((r) = required)                                                                                                                            | Description                                                                                                                                                                                                                                                                                                               |
|:------------|:------------------------------------------------------------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| GET         | config (a)                                                        | -                                                                                                                                                      | Get current extension configuration values.                                                                                                                                                                                                                                                                               |
| PUT         | config (a)                                                        | Body: Updated config values (JSON) (r)                                                                                                                 | Update config values.                                                                                                                                                                                                                                                                                                     |
| POST        | service (a)                                                       | Query Parameter "url" (r)                                                                                                                              | Register a standalone AAS service (e.g., FA³ST) to this extension.                                                                                                                                                                                                                                                        |
| DELETE      | service (a)                                                       | Query Parameter "url" (r)                                                                                                                              | Unregister an AAS service (e.g., FA³ST) from this extension, possibly shutting down the service if it has been started internally.                                                                                                                                                                                        |
| POST        | environment (a)                                                   | Query Parameters: "environment": Path to AAS environment (r), "port": Port of service to be created , "config": Path of AAS service configuration file | Create a new AAS service. Either (http) "port" or "config" must be given to ensure communication with the AAS service via an HTTP endpoint on the service's side. This command returns the URL of the newly created AAS service on success, which can be used to remove the service using the interface "DELETE /service" |
| GET         | selfDescription                                                   | -                                                                                                                                                      | Return self description of extension.                                                                                                                                                                                                                                                                                     |

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

In this section, dependencies from EDC and third-party are listed. (Implementation for test runs are not shown)
<details>
<summary>EDC-Extension-for-AAS</summary>

| Name                                          | Description                                                                                         |
|:----------------------------------------------|:----------------------------------------------------------------------------------------------------|
| public-api-management (local)                 | Centralized http authentication request filter                                                      |
| data-plane-aas (local)                        | Allowing easy communication with AAS services through AAS data addresses                            |
| de.fraunhofer.iosb.ilt.faaast.service:starter | [FA³ST Service](https://github.com/FraunhoferIOSB/FAAAST-Service) to start AAS services internally. |
| org.eclipse.edc:http-spi                      | OkHttp3 Fields                                                                                      |
| org.eclipse.edc:management-api                | EDC Asset/Contract Management                                                                       |

</details>
<details>
<summary>Client Extension</summary>  

| Name                                        | Description                                                              |
|:--------------------------------------------|:-------------------------------------------------------------------------|
| public-api-management (local)               | Centralized http authentication request filter                           |
| data-plane-aas (local)                      | Allowing easy communication with AAS services through AAS data addresses |
| org.eclipse.edc:connector-core              | PolicyService                                                            |
| org.eclipse.edc:control-plane-contract      | Observe contract negotiations                                            |
| org.eclipse.edc:control-plane-transform     | Type transformers                                                        |
| org.eclipse.edc:data-plane-http-spi         | HttpDataAddress                                                          |
| org.eclipse.edc:dsp-catalog-http-dispatcher | EDC constants                                                            |
| org.eclipse.edc:json-ld-lib                 | JsonLD expansion                                                         |
| org.eclipse.edc:management-api              | EDC WebService for registering endpoints                                 |

</details>
<details>
<summary>Data-Plane-AAS</summary>

| Name                                      | Description                                                                        |
|:------------------------------------------|:-----------------------------------------------------------------------------------|
| org.eclipse.edc:data-plane-spi            | Data-plane functionality                                                           |
| org.eclipse.edc:lib                       | OkHttp3 Fields + EdcHttpClient implementation                                      |
| org.eclipse.digitaltwin.aas4j:aas4j-model | [Eclipse AAS4J java model](https://github.com/eclipse-aas4j/aas4j/tree/main/model) |

</details>
<details>
<summary>Public API Management Extension</summary>  

| Name                     | Description            |
|:-------------------------|:-----------------------|
| org.eclipse.edc:auth-spi | EDC Authentication SPI |

</details>

### Configuration Values

<details>
<summary>Common Configurations</summary>

| Key                                               | Values            | Description                                                                                                                                                                            |
|:--------------------------------------------------|:------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| edc.dataplane.aas.acceptAllSelfSignedCertificates | True/<u>False</u> | Accept self-signed certificates from ALL AAS services (internal+external, provider+consumer)                                                                                           |
| edc.dataplane.aas.acceptOwnSelfSignedCertificates | True/<u>False</u> | Accept self-signed certificates from registered services (example: Creating AAS service in extension -> extension registers service at dataplane to allow its self-signed certificate) |

</details>
<details>
<summary>EDC-Extension-for-AAS</summary>

| Key                               | Value Type        | Description                                                                                                                                                                      |
|:----------------------------------|:------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| edc.aas.defaultAccessPolicyPath   | path              | Path to an access policy file (JSON). This policy will be used as the default access policy for all assets created after the configuration value has been set.                   |
| edc.aas.defaultContractPolicyPath | path              | Path to a contract policy file (JSON). This policy will be used as the default contract policy for all assets created after the configuration value has been set.                |
| edc.aas.exposeSelfDescription     | boolean           | Whether the Self Description should be exposed on {edc}/api/selfDescription. When set to False, the selfDescription is still available for authenticated requests. Default: True |
| edc.aas.localAASModelPath         | path              | A path to a serialized AAS environment compatible to specification version 3.0RC01 (see: https://github.com/FraunhoferIOSB/FAAAST-Service/blob/main/README.md)                   |
| edc.aas.localAASServiceConfigPath | path              | Path to AAS config for locally started AAS service. Required, if localAASServicePort is not defined, but localAASModelPath is defined.                                           |
| edc.aas.localAASServicePort       | Open network port | Port to locally created AAS service. Required, if localAASModelPath is defined and localAASServiceConfigPath is not defined.                                                     |
| edc.aas.onlySubmodels             | boolean           | (Provider) Only register submodels of AAS services. Default: False                                                                                                               |
| edc.aas.remoteAasLocation         | URL               | Register a URL of an AAS service (such as FA³ST) that is already running and is conformant with official AAS API specification                                                   |
| edc.aas.syncPeriod                | number in seconds | Time period in which AAS services should be polled for structural changes (added/deleted elements etc.). Default: 5 (seconds).                                                   |

</details>
<details>
<summary>Client Extension</summary>  

| Key                                      | Value Type              | Description                                                                                                                                                                   |
|:-----------------------------------------|:------------------------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| edc.client.acceptAllProviderOffers       | True/<u>False</u>       | Accept any contractOffer offered by all provider connectors on automated contract negotiation (e.g., trusted provider)                                                        |
| edc.client.acceptedPolicyDefinitionsPath | path                    | Path pointing to a JSON-file containing acceptable PolicyDefinitions for automated contract negotiation in a list (only policies must match in a provider's PolicyDefinition) |
| edc.client.waitForAgreementTimeout       | whole number in seconds | How long should the extension wait for an agreement when automatically negotiating a contract? Default value is 10(s).                                                        |
| edc.client.waitForTransferTimeout        | whole number in seconds | How long should the extension wait for a data transfer when automatically negotiating a contract? Default value is 10(s).                                                     |

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
- AAS data-plane for EDC &#x2713;
- Docker Hub container deployment
