# EDC Extension for Asset Administration Shell [![Build Status](https://github.com/FraunhoferIOSB/EDC-Extension-for-AAS/actions/workflows/gradle.yml/badge.svg)](https://github.com/FraunhoferIOSB/EDC-Extension-for-AAS/actions)

This [Eclipse Dataspace Connector (EDC)](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector) extension provides an easy way to share an [Asset Administration Shell (AAS)](https://www.plattform-i40.de/SiteGlobals/IP/Forms/Listen/Downloads/EN/Downloads_Formular.html?cl2Categories_TechnologieAnwendungsbereich_name=Verwaltungsschale) model via the EDC. 


## Version compatibility
| Specification | Version |
|:--| -- |
| Eclipse Dataspace Connector | v0.0.1-milestone-7
| AAS - Details of the Asset Administration Shell - Part 1<br />The exchange of information between partners in the value chain of Industrie 4.0 | Version 3.0RC01<br />(based on [admin-shell-io/java-model](https://github.com/admin-shell-io/java-model))

## Repo Structure
The repository contains several material:
- `config`: Checkstyle files for code formatting
- `edc-extension4aas`: Source code for the extension
- `example`: Example use case for the edc-extension4aas with a preconfigured EDC launcher. 

<!-- ------------------Template Section --------------------------- -->
## Example Usage

For a data transfer example using two connectors communicating with the IDS protocol, check the [Example's README](example/README.md).

## Functionality

AAS data can be shared over the EDC by linking an EDC Asset to the HTTP endpoint of the AAS element. Additionally, contracts have to be defined for each element.
In order to minimize configuration effort and prevent errors, this extension is able to link running AAS into EDC Assets. Furthermore, this extension can also start AAS by reading an AAS model. A default contract can be chosen to be applied for all elements. For critical elements, additional contracts can be placed. 
External changes to the model of an AAS are automatically synchronized by the Extension.

### Use Cases

Provide digital twin (AAS) data to business partners in Data Spaces like Catena-X or Manufacturing-X

## Technical Details

### Interfaces

| HTTP Method | Interface (edc:1234/api/...) ((a) = only for authenticated users) | Parameters ((r) = required) | Description |
| :----| :----| :---- | :-------------------- |
| GET | config (a) | - | Get current extension configuration values.
| PUT | config (a) | Body: Updated config values (JSON) (r) | Update config values.
| POST | client (a) | Query Parameter "url" (r) | Register a standalone AAS service (e.g., FA³ST) to this extension.
| DELETE | client (a) | Query Parameter "url" (r) | Unregister an AAS service (e.g., FA³ST) from this extension, possibly shutting down the service if it has been started internally.
| POST | environment (a) | Query Parameter "environment": Path to new AAS environment (r), Query Parameter "port": Port of service to be created , Query Parameter "config": Path of AAS service configuration file | Create a new AAS service. Either (http) "port" or "config" must be given to ensure communication with the AAS service via an HTTP endpoint on the service's side. This command returns the URL of the newly created AAS service on success, which can be used to remove the service using the interface "DELETE /client"
| POST | aas (a) | Query Parameter "requestUrl": URL of AAS service to be updated (r), request body: AAS element (r) | Forward POST request to provided host in requestUrl. If requestUrl is an AAS service that is registered at this EDC, synchronize assets and self description as well.
| DELETE | aas (a) | Query Parameter requestUrl: URL of AAS service to be updated (r)| Forward DELETE request to provided host in requestUrl. If requestUrl is an AAS service that is registered at this EDC, synchronize assets and self description as well.
| PUT | aas (a) | Query Parameter "requestUrl": URL of AAS service to be updated (r), request body: AAS element (r) | Forward PUT request to provided host in requestUrl.
| GET | selfDescription | - | Return self description of extension.

### Dependencies

| Name | Description |
| :----| :-----------|
| de.fraunhofer.iosb.ilt.faaast.service:starter | [FA³ST Service](https://github.com/FraunhoferIOSB/FAAAST-Service) to start AAS services internally.
| io.admin-shell.aas:dataformat-json | [admin-shell-io java serializer](https://github.com/admin-shell-io/java-serializer) (de-)serialize AAS models
| io.admin-shell.aas:model | [admin-shell-io java model](https://github.com/admin-shell-io/java-model) (de-)serialize AAS models
| org.eclipse.edc:data-management-api | EDC asset/contract management
| com.squareup.okhttp3:okhttp | Send HTTP requests to AAS services
| jakarta.ws.rs:jakarta.ws.rs-api | provides HTTP endpoints of extension

### Configurations

| Key | Value Type | Description |
| :----| :-----------| :------|
|edc.aas.remoteAasLocation | URL | A URL of an AAS service (such as FA³ST service) that is already running and is conformant to official AAS API specification|
|edc.aas.localAASModelPath| path | A path to a serialized AAS environment compatible to specification version 3.0RC01 (see: https://github.com/FraunhoferIOSB/FAAAST-Service/blob/main/README.md)|
|edc.aas.localAASServicePort| (1-65535)| Port to locally created AAS service. Required, if localAASModelPath is defined and localAASServiceConfigPath is not defined.|
|edc.aas.localAASServiceConfigPath|path|Path to AAS config for locally started AAS service. Required, if localAASServicePort is not defined, but localAASModelPath is defined.|
|edc.aas.syncPeriod |whole number in seconds |Time period in which AAS services should be polled for structural changes (added/deleted elements etc.). Default value is 5 (seconds). Note: This configuration value is only read on startup, the synchronization period cannot be changed at runtime.|
|edc.aas.exposeSelfDescription| True/False| Whether the Self Description should be exposed on {edc}/api/selfDescription. When set to False, the selfDescription is still available for authenticated requests.|
|edc.aas.defaultAccessPolicyPath|path |Path to an access policy file (JSON). This policy will be used as the default access policy for all assets created after the configuration value has been set.|
|edc.aas.defaultContractPolicyPath|path |Path to a contract policy file (JSON). This policy will be used as the default contract policy for all assets created after the configuration value has been set.|
|edc.aas.waitForAgreementTimeout|whole number in seconds |How long should the extension wait for an agreement when automatically negotiating a contract? Default value is 10(s).|
|edc.aas.waitForTransferTimeout|whole number in seconds |How long should the extension wait for a data transfer when automatically negotiating a contract? Default value is 10(s).|

## Terminology
| Term | Description |
| :----| :-----------|
|AAS | Asset Administration Shell (see [AAS reading guide](https://www.plattform-i40.de/IP/Redaktion/DE/Downloads/Publikation/Asset_Administration_Shell_Reading_Guide.html) or [AAS specification part 1](https://www.plattform-i40.de/IP/Redaktion/DE/Downloads/Publikation/Details_of_the_Asset_Administration_Shell_Part1_V3.html)) |
|FA³ST Service | Open Source java implementation of the AAS part 2 see on [GitHub](https://github.com/FraunhoferIOSB/FAAAST-Service)|

## Roadmap
Features in development:
- Graphical interface to simplify providing and requesting AAS
- Built-in client to request AAS data from other EDC (automatic contract negotiation)
- Docker Hub container deployment
