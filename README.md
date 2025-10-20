# EDC Extension for Asset Administration Shell [![Build Status](https://github.com/FraunhoferIOSB/EDC-Extension-for-AAS/actions/workflows/gradle.yml/badge.svg)](https://github.com/FraunhoferIOSB/EDC-Extension-for-AAS/actions)

This [Eclipse Dataspace Connector (EDC)](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector) extension
provides an easy way to share
an [Asset Administration Shell (AAS)](https://www.plattform-i40.de/SiteGlobals/IP/Forms/Listen/Downloads/EN/Downloads_Formular.html?cl2Categories_TechnologieAnwendungsbereich_name=Verwaltungsschale)
model via the EDC.

## Version compatibility

| Specification                                                                                                                                                                                                                                                                | Version                                                                                                      |
|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| [Eclipse Dataspace Connector](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector)                                                                                                                                                                              | v0.13.0, v0.13.2, v0.14.0                                                                                    |
| [AAS - Details of the Asset Administration Shell - Part 1](https://www.plattform-i40.de/IP/Redaktion/EN/Downloads/Publikation/Details_of_the_Asset_Administration_Shell_Part1_V3.html)<br />The exchange of information between partners in the value chain of Industrie 4.0 | AAS Specs – Part 1 V3.0 (final)<br/>(based on [eclipse-aas4j/aas4j](https://github.com/eclipse-aas4j/aas4j)) |

## Repo Structure

The repository contains several material:
- `extensions`: Extensions to the Eclipse Dataspace Components (EDC)
  - `control-plane`: Extensions to the EDC control-plane
    - `edc-extension4aas`: The AAS extension
      - `client`: The client extension: Automated contract negotiation from HTTP endpoints
      - `public-api-management`: Managing outward facing endpoints (http) which require no authentication
  - `data-plane`: Extensions to the EDC data-plane
    - `data-plane-aas`: AAS data plane implementation (following HTTP data plane)
  - `edc-connector-client`: Communicate with the EntityStores of a remote control-plane instance over its management API
- `launchers`: several connector configurations: control-planes, data-planes, standalone extension, tractus-x, ...
- `samples`: Example use cases for the extensions
- `misc`: Supplemental files for the project
  - `checkstyle`: Checkstyle files for code formatting
  - `http`: Postman and Bruno files for HTTP requests (coming soon :tm: ) 

<!-- ------------------Template Section --------------------------- -->

## Example Usage

For a data transfer example using two connectors communicating with the DSP protocol, check
the [example's README](example/README.md).

## Functionality

AAS data can be shared over the EDC by linking an EDC Asset to the HTTP endpoint of the AAS element/submodel.
Additionally,
contracts have to be defined for each element. In order to minimize configuration effort and prevent errors, this
extension is able to link running AAS into EDC Assets by connecting to AAS repositories or AAS registries. Furthermore,
this
extension can also start an AAS repository by reading an AAS
model (AASX / JSON). A default contract can be chosen to be applied for all elements. For critical elements, additional
contracts can
be placed via API. External changes to the model of an AAS are automatically synchronized by the extension, reducing
management of metadata significantly.

Additionally, a client extension providing API calls for aggregations of processes such as contract negotiation and data
transfer is available. The result is a one-click negotiation and data transfer, ideal for SME or individuals.

### Use Cases

Provide digital twin (AAS) data to business partners in Data Spaces. Data Providers can share their AAS Repository / AAS
Registry with other participants.

## Technical Details

### Interfaces

<details>

<summary>Provider Interfaces</summary>

| HTTP Method | Interface (edc:1234/api/...) ((a) = only for authenticated users) | Parameters ((r) = required)                                                                                                                                                  | Description                                                                                                                                                                                                                                            |
|:------------|:------------------------------------------------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| GET         | config (a)                                                        | -                                                                                                                                                                            | Get current extension configuration values.                                                                                                                                                                                                            |
| PATCH       | config (a)                                                        | Body: Updated config values (JSON) (r)                                                                                                                                       | Update config values.                                                                                                                                                                                                                                  |
| POST        | service (a)                                                       | Query Parameter "url"                                                                                                                                                        | Register a standalone AAS service (e.g., FA³ST) to this extension. The service can now be supplied in JSON format with a selection of AAS elements to register and access and usage policies per AAS element. See changelog.md for an example of this. |
| DELETE      | service (a)                                                       | Query Parameter "url" (r)                                                                                                                                                    | Unregister an AAS service (e.g., FA³ST) from this extension, possibly shutting down the service if it has been started internally.                                                                                                                     |
| POST        | registry (a)                                                      | Query Parameter "url" (r)                                                                                                                                                    | Register an AAS registry (e.g., FA³ST) to this extension.                                                                                                                                                                                              |
| DELETE      | registry (a)                                                      | Query Parameter "url" (r)                                                                                                                                                    | Unregister an AAS registry (e.g., FA³ST) from this extension.                                                                                                                                                                                          |
| POST        | environment (a)                                                   | Query Parameters: "environment": Path to AAS environment (r), "port": HTTP communication port of service to be created , "config": Path to an AAS service configuration file | Start a new AAS service internally. If a port is provided explicitly, this port will be used for communications with the AAS service.                                                                                                                  |
| GET         | selfDescription                                                   | Query Parameter "url"                                                                                                                                                        | Return the self-description of all registered services/registries of this extension. If url is defined, return only this self-description.                                                                                                             |

</details>

<details>
<summary>Client Interfaces</summary>

| HTTP Method | Interface (edc:1234/api/automated/...) ((a) = only for authenticated users) | Parameters ((r) = required)                                                                                                                                        | Description                                                                                                                                                                                                                                                                                                                                        |
|:------------|:----------------------------------------------------------------------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| POST        | negotiate (a)                                                               | Query Parameter "providerUrl": URL (r), Query Parameter "providerId": String (r), Query Parameter "assetId": String (r), Query Parameter "dataDestinationUrl": URL | Perform an automated contract negotiation with a provider (given provider URL and ID) and get the data stored for the specified asset. Optionally, a data destination URL can be specified where the data is sent to instead of the extension's log, or a data address can be provided through the request body which defines the data destination |
| GET         | dataset (a)                                                                 | Query Parameter "providerUrl": URL (r), Query Parameter "assetId": String (r), Query Parameter "providerId": String (r)                                            | Get dataset from the specified provider's catalog that contains the specified asset's policies.                                                                                                                                                                                                                                                    |
| POST        | negotiateContract (a)                                                       | request body: org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest (r)                                                                         | Using a contractRequest (JSON in http request body), negotiate a contract. Returns the corresponding agreementId on success.                                                                                                                                                                                                                       |
| GET         | transfer (a)                                                                | Query Parameter "providerUrl": URL (r), Query Parameter "agreementId": String (r), Query Parameter "assetId": String (r), Query Parameter "dataDestinationUrl"     | Submits a data transfer request to the providerUrl. On success, returns the data behind the specified asset. Optionally, a data destination URL can be specified where the data is sent to instead of the extension's log.                                                                                                                         |
| POST        | acceptedPolicies (a)                                                        | request body: List of PolicyDefinitions (JSON) (r)                                                                                                                 | Adds the given PolicyDefinitions to the accepted PolicyDefinitions list (Explanation: On fully automated negotiation, the provider's PolicyDefinition is matched against the consumer's accepted PolicyDefinitions list. If any PolicyDefinition fits the provider's, the negotiation continues.) Returns "OK"-Response if requestBody is valid.   |
| GET         | acceptedPolicies (a)                                                        | -                                                                                                                                                                  | Returns the client extension's accepted policy definitions for fully automated negotiation.                                                                                                                                                                                                                                                        |
| DELETE      | acceptedPolicies (a)                                                        | request body: PolicyDefinition: PolicyDefinition (JSON) (r)                                                                                                        | Updates the client extension's accepted policy definition with the same policyDefinitionId as the request.                                                                                                                                                                                                                                         |
| PUT         | acceptedPolicies (a)                                                        | request body: PolicyDefinitionId: String (JSON) (r)                                                                                                                | Deletes a client extension's accepted policy definition with the same policyDefinitionId as the request.                                                                                                                                                                                                                                           |

</details>

### Dependencies

In this section, dependencies from EDC and third-party are listed. (Dependencies for tests are not shown)
<details>
<summary>EDC-Extension-for-AAS</summary>

| Name                                          | Description                                                                                         |
|:----------------------------------------------|:----------------------------------------------------------------------------------------------------|
| public-api-management (local)                 | Centralized http authentication request filter                                                      |
| aas-lib (local)                               | Provides common objects for AAS data plane and AAS extension                                        |
| de.fraunhofer.iosb.ilt.faaast.service:starter | [FA³ST Service](https://github.com/FraunhoferIOSB/FAAAST-Service) to start AAS services internally. |
| org.eclipse.edc:http-lib                      | OkHttp3 Fields                                                                                      |
| org.eclipse.edc:data-plane-http-spi           | HTTPDataAddress                                                                                     |
| org.eclipse.edc:json-ld-spi                   | Policy action attributes                                                                            |
| org.eclipse.edc:asset-spi                     | EDC Asset                                                                                           |
| org.eclipse.edc:contract-spi                  | EDC Contract/Policy                                                                                 |

</details>
<details>
<summary>Client Extension</summary>  

| Name                                        | Description                                    |
|:--------------------------------------------|:-----------------------------------------------|
| public-api-management (local)               | Centralized http authentication request filter |
| org.eclipse.edc:connector-core              | PolicyService                                  |
| org.eclipse.edc:control-plane-contract      | Observe contract negotiations                  |
| org.eclipse.edc:control-plane-transform     | Type transformers                              |
| org.eclipse.edc:data-plane-http-spi         | HttpDataAddress                                |
| org.eclipse.edc:dsp-catalog-http-dispatcher | EDC constants                                  |
| org.eclipse.edc:json-ld-lib                 | JsonLD expansion                               |

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
<summary>EDC-Connector-Client</summary>

| Name                                    | Description                                                  |
|:----------------------------------------|:-------------------------------------------------------------|
| aas-lib (local)                         | Provides common objects for AAS data plane and AAS extension |
| org.eclipse.edc:asset-spi               | EDC Asset                                                    |
| org.eclipse.edc:contract-spi            | EDC Policy/Contract                                          |
| org.eclipse.edc:transform-lib           | Transformers to / from JSON                                  |
| org.eclipse.edc:control-plane-transform | Transformers to / from JSON                                  |
| org.eclipse.edc:json-ld                 | JSON-LD expansion / compaction                               |
| org.eclipse.edc:runtime-core            | Core services                                                |
| org.eclipse.edc:connector-core          | Core services                                                |

</details>
<details>
<summary>Public API Management Extension</summary>  

| Name                     | Description            |
|:-------------------------|:-----------------------|
| org.eclipse.edc:auth-spi | EDC Authentication SPI |

</details>

<details>
<summary>AAS Library</summary>  

| Name                                      | Description                                                                        |
|:------------------------------------------|:-----------------------------------------------------------------------------------|
| org.eclipse.edc:asset-spi                 | Asset, DataAddress                                                                 |
| org.eclipse.digitaltwin.aas4j:aas4j-model | [Eclipse AAS4J java model](https://github.com/eclipse-aas4j/aas4j/tree/main/model) |

</details>

### Configuration Values

<details>
<summary>EDC-Extension-for-AAS</summary>

| Key (edc.aas.)              | Value Type        | Description                                                                                                                                                                      |
|:----------------------------|:------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| allowSelfSignedCertificates | boolean           | Whether to allow self-signed certificates for own AAS services/registries.                                                                                                       |
| defaultAccessPolicyPath     | path              | Path to an access policy file (JSON). This policy will be used as the default access policy for all assets created after the configuration value has been set.                   |
| defaultContractPolicyPath   | path              | Path to a contract policy file (JSON). This policy will be used as the default contract policy for all assets created after the configuration value has been set.                |
| exposeSelfDescription       | boolean           | Whether the Self Description should be exposed on {edc}/api/selfDescription. When set to False, the selfDescription is still available for authenticated requests. Default: True |
| localAASModelPath           | path              | A path to a serialized AAS environment compatible to specification version 3.0RC01 (see: https://github.com/FraunhoferIOSB/FAAAST-Service/blob/main/README.md)                   |
| localAASServiceConfigPath   | path              | Path to AAS config for locally started AAS service. Required, if localAASServicePort is not defined, but localAASModelPath is defined.                                           |
| localAASServicePort         | Open network port | Port to locally created AAS service. Required, if localAASModelPath is defined and localAASServiceConfigPath is not defined.                                                     |
| onlySubmodels               | boolean           | (Provider) Only register submodels of AAS services. Default: True                                                                                                                |
| remoteAasLocation           | URL               | Register a URL of an AAS service (such as FA³ST) that is already running and is conformant with official AAS API specification                                                   |
| useAasDataPlane             | boolean           | Whether to use AAS data plane or HTTP DataPlane to register AAS elements. (Default: False)                                                                                       |
| syncPeriod                  | number in seconds | Time period in which AAS services should be polled for structural changes (added/deleted elements etc.). Default: 50 (seconds).                                                  |

</details>
<details>
<summary>Client Extension</summary>  

| Key (edc.client.)             | Value Type              | Description                                                                                                                                                                   |
|:------------------------------|:------------------------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| acceptAllProviderOffers       | boolean                 | Accept any contractOffer offered by all provider connectors on automated contract negotiation (e.g., trusted provider)                                                        |
| acceptedPolicyDefinitionsPath | path                    | Path pointing to a JSON-file containing acceptable PolicyDefinitions for automated contract negotiation in a list (only policies must match in a provider's PolicyDefinition) |
| waitForAgreementTimeout       | whole number in seconds | How long should the extension wait for an agreement when automatically negotiating a contract? Default value is 20(s).                                                        |
| waitForCatalogTimeout         | whole number in seconds | How long should the extension wait for a catalog? Default value is 20(s).                                                                                                     |
| waitForTransferTimeout        | whole number in seconds | How long should the extension wait for a data transfer when automatically negotiating a contract? Default value is 20(s).                                                     |

</details>

<details>
<summary>AAS Data Plane Extension</summary>

| Key (edc.dataplane.aas.)            | Values Type | Description                                                                                                                                   |
|:------------------------------------|:------------|:----------------------------------------------------------------------------------------------------------------------------------------------|
| acceptOwnSelfSignedCertificates     | boolean     | Accept self-signed certificates from own AAS services <u>if the configured EDC is a data provider.</u>                                        |
| acceptForeignSelfSignedCertificates | boolean     | Accept self-signed certificates from ALL AAS services <u>if the configured EDC shall send data to services with self-signed certificates.</u> |

</details>

## Terminology

| Term          | Description                                                                                                                                                                                                                                                                                                        |
|:--------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| AAS           | Asset Administration Shell (see [AAS reading guide](https://industrialdigitaltwin.org/wp-content/uploads/2022/11/2022-11-03_IDTA_AAS-Reading-Guide.pdf) or [AAS specification part 1](https://www.plattform-i40.de/IP/Redaktion/DE/Downloads/Publikation/Details_of_the_Asset_Administration_Shell_Part1_V3.html)) |
| FA³ST Service | Open Source java implementation of the AAS part 2 [see on GitHub](https://github.com/FraunhoferIOSB/FAAAST-Service)                                                                                                                                                                                                |

## Roadmap

Features in development:

- Graphical interface to simplify providing and requesting AAS (
  see: https://github.com/FraunhoferIOSB/EDC-Extension-for-AAS-Dashboard) (&#x2713; update required)
- AAS data-plane for EDC &#x2713;
- Docker Hub container deployment &#x2713;
- Support for AAS Registries: Share all AAS from AAS Registry &#x2713;
- Client DPP Viewer: Directly view the requested DPP (AAS format)
- Listen for updates on FA³ST message bus
