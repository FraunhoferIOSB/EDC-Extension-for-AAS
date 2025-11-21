# AAS Extension

This extension implements the logic to automatically register and keep AAS in the EDC. It can be
run [standalone](../../../launchers/standalone) or as part of the [control-plane](../../../launchers/provider).

## Configuration

There are two types of configuration variables:

- Initialization variables are used for bootstrapping the extension. For example, one could supply a `remoteAasLocation`
  which the extension automatically registers on startup.
- Runtime variables are used during the execution of the extension. This does not necessarily mean they are
  runtime-modifiable.

### Initialization

| Key (edc.aas.)            | Value Type   | Description                                                                                                                                                       |
|:--------------------------|:-------------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| defaultAccessPolicyPath   | path         | Path to an access policy file (JSON). This policy will be used as the default access policy for all assets created after the configuration value has been set.    |
| defaultContractPolicyPath | path         | Path to a contract policy file (JSON). This policy will be used as the default contract policy for all assets created after the configuration value has been set. |
| localAASModelPath         | path         | A path to an AAS file compatible to the specification (see: https://github.com/FraunhoferIOSB/FAAAST-Service/blob/main/README.md)                                 |
| localAASServiceConfigPath | path         | Path to AAS config for locally started AAS service. Required, if localAASServicePort is not defined, but localAASModelPath is defined.                            |
| localAASServicePort       | network port | Port to locally created AAS service. Required, if localAASModelPath is defined and localAASServiceConfigPath is not defined.                                      |
| remoteAasLocation         | URL          | Register a URL of an AAS service (such as FA³ST) that is already running and is conformant with official AAS API specification                                    |

### Runtime

| Key (edc.aas.)              | Value Type        | Description                                                                                 |
|:----------------------------|:------------------|:--------------------------------------------------------------------------------------------|
| allowSelfSignedCertificates | boolean           | Whether to allow self-signed certificates for own AAS services/registries.                  |
| exposeSelfDescription       | boolean           | Whether to expose the self-description on {edc}/api/selfDescription. Default: True          |
| onlySubmodels               | boolean           | (Provider) Only register submodels of AAS services. Default: True                           |
| useAasDataPlane             | boolean           | Whether to use AAS data-plane or HTTP DataPlane to register AAS elements. (Default: True)   |
| syncPeriod                  | number in seconds | Time period in which AAS remote servers should be polled for changes Default: 50 (seconds). |

## Interfaces

The AAS extension publishes all of its API over the EDC api endpoint that can be configured by `web.http.port`,
`web.http.path`, `web.http.default.auth.type`, `web.http.default.auth.key.alias` and/or `web.http.default.auth.key`. All
endpoints are authenticated if the respective EDC endpoint requires authentication, except for the self-description
endpoint, which can be configured to be exposed without any authentication.

For the specific API calls, please check out the Bruno collection located in misc/http

### Dependencies

| Name                                          | Description                           |
|:----------------------------------------------|:--------------------------------------|
| public-api-management (local)                 | Centralized http auth request filters |
| aas-lib (local)                               | Provides common AAS objects           |
| de.fraunhofer.iosb.ilt.faaast.service:starter | Starting the FA³ST service internally |
| org.eclipse.edc:asset-spi                     | EDC Asset                             |
| org.eclipse.edc:contract-spi                  | EDC Contract/Policy                   |
| org.eclipse.edc:data-plane-http-spi           | HTTPDataAddress                       |
| org.eclipse.edc:http-lib                      | OkHttp3                               |
| org.eclipse.edc:json-ld-spi                   | Policy action attributes              |
| org.eclipse.edc:util-lib                      | Utilities like getFreePort            |
