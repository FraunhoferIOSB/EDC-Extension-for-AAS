# Changelog

## Current development version

Compatibility: **Eclipse Dataspace Connector v0.8.1, v0.9.0, v0.9.1**

**New Features**

* AAS registries (spec, example) can now be registered at the extension.
    * Add a FA³ST Registry / AAS registry by URL
    * The extension reads the shell-/submodel-descriptors and registers assets for their endpoints
    * The information (id, idShort, ...) displayed in the self-description comes only from the registry itself, the
      service behind the endpoint is not queried
    * When a contract is negotiated for one of those elements, the endpoint provided by the
      shell-/submodel-descriptor
      is used as data source for the data transfer
* Add AAS Authentication schemes
    * If an external AAS service/registry needs authentication, this can be configured when registering the
      service/registry at the extension
    * example: `{ "type":"basic", "username": "admin", "password": "administrator" }`
* Allow for AAS operation invokations by clients on provider AAS.
    * See example in postman collection (in the client directory)

**Bugfixes**

**Miscellaneous**

* Important: URLs for an AAS Service / Registry must now be provided fully until the /shells, /submodels and
  /concept-descriptions endpoints
    * Until last version, ´/api/v3.0´ was appended to the access URL of each AAS service/registry.
    * Now, a URL must be provided such that appending /shells, /submodels and /concept-descriptions yields the
      respective resources.
* The synchronization of the EDC AssetIndex/ContractStore to the AAS services/registries is updated to a pipeline
  architecture.
* The extension does not use custom AAS models for internal persistence any longer
    * Instead, the nested structure of the AAS environment is now stored in an 'environment asset'.
        * This asset is kept by the extension for the self-description
        * Its elements are added to the assetIndex (and contractStore/policyDefinitionStore)
    * On AAS environment updates, this nested asset is updated and these updates are propagated to the EDC components as
      described above.
    * This makes the extension not rely on custom data classes which can be invalidated through an update of AAS or EDC
    * It also makes (de)serialization of AAS environments easier
* Updated FA³ST to version v1.1.0
* Updated EDC to version 0.8.1
* Removed custom dependency injection because of transitive dependency issue from FA³ST service
    * This was in `example/build.gradle.kts`

## V2.1.1

This version is compatible to **Eclipse Dataspace Connector v0.8.0**

**New Features**

* Dynamically building AAS access URLs using Reference Chains [Submodel x, Collection y, Element z], a concept from AAS
  the specification
* Accepting self-signed certificates now
  optional (`edc.dataplane.aas.acceptOwnSelfSignedCertificates`, `edc.dataplane.aas.acceptAllSelfSignedCertificates`)
* SubmodelElementList is now a model element inside the SelfDescription (AASv3 change)
* Synchronizer rate can now be changed at runtime via configuration updates (time unit still in seconds)

* New extension: data-plane-aas
    * Provides custom AAS data source and data sink
    * Communicate with AAS over AASDataAddress (see above, reference chains)
    * Allow communication with AAS with self-signed TLS certificates (configurable)
    * Use default EdcHttpClient if not self-signed

**Bugfixes**

* Adding external AAS services with self-signed certificates using configuration value edc.aas.remoteAASLocation is now
  possible
* Unregistering an external AAS service no longer throws IllegalArgumentException
* DataTransfer with AAS DataSource no longer throws IOException: closed
* Test runs on Microsoft's OS no longer fail
* Update older docker example files to support new version of extension

**Miscellaneous**

* In the development version, HTTP Push data transfer to AAS services with self-signed certificates is possible
    * This is only for testing
    * Will be removed in release version
    * Only works if the provider "knows" the consumer AAS service
* EDC4AAS extension now uses EDC monitor directly instead of wrapping it with custom logger
* Since the AAS model parsing got more complex, it was extracted out of the AAS agent
* Added tests for self-signed certificate retriever

## V2.0.0

This version is compatible to **Eclipse Dataspace Connector v0.6.2**

**New Features**

* Support of **Eclipse Dataspace Connector v0.6.2**
* **AAS was updated to model V3**
* Option to offer Submodels instead of all SubmodelElements.

**Bugfixes**

* Allow data transfer with self-signed certificates (FA³ST DataSource)

**Miscellaneous**

* Update postman requests
* !Change API port for self-description to 8281/9291 in examples!

## V1.0.0-alpha5

This version is compatible to **Eclipse Dataspace Connector v0.4.1**

**New Features**

* Support of **Eclipse Dataspace Connector v0.4.1**
* Extracted automated client negotiation into own extension (/client)

**Bugfixes**

* Fix: Case where submodels not offered by provider
* Fix: Wrong nested submodelCollectionElement URLs
* Fix: Empty catalog response handling

**Miscellaneous**

* Update postman requests
* Cleanup and fix response values
* Update docker example's java version

## V1.0.0-alpha4

This version is compatible to **Eclipse Dataspace Connector v0.3.0**

**New Features**

* Support of **Eclipse Dataspace Connector v0.3.0**

**Bugfixes**

* Fix typo in postman request

**Miscellaneous**

* Update changelog.md, README.md

## V1.0.0-alpha3

This version is compatible to **Eclipse Dataspace Connector v0.0.1-Milestone 8**

**New Features**

* Support of **Eclipse Dataspace Connector v0.0.1-Milestone 8**

* Client Service
    * Add custom HTTP endpoints for data transfers
* Add TLS/SSL config files
    * Allows HTTPS communication between EDCs with non-self-signed certificates

**Bugfixes**

* Postman collection
    * Fix some old requests
* ContractDefinitions created by extension
    * Validity was set to max float value, is now set to 1 year (default)
* Dependencies
    * Add data flow controller dependency
* Remove waiting for dataTransfer when specifying a custom data transfer target address
* Remove initial AAS service synchronization delay on connector startup
* Config
    * Allow communication with DataDashboard (added to all configs except debug config)
* Synchronizer
    * Extract AAS-EDC synchronizer to new class
* AAS service
    * Prefer explicit service port to port inside AAS service configuration file
* Uploading accepted contracts required another dependency
* Update metamodel version

**Miscellaneous**

* Add missing license headers
* Client
    * Reuse contractAgreements when available
* Clean up configuration class
* Improve test coverage
* Add missing license headers

## V1.0.0-alpha2

This version is compatible to **Eclipse Dataspace Connector v0.0.1-Milestone 7**

**New Features**

* Client Service
    * Add functionality for automatically negotiating and requesting data from a provider EDC
        * On automatic negotiation, provider contractOffers are matched against a set of own contractOffers (
          acceptedContractOffers)
        * Provider data is sent to a specific client HTTP endpoint using a new authentication key for each transaction (
          derived from the agreementId)
    * Add functionality for manually
        * fetching a set of provider contractOffers for a provider asset,
        * negotiating a contract with a provider using a contractOffer, returning an agreementId or an error message,
        * getting data of a negotiated contract from a provider.

**Bugfixes**

* CustomAuthenticationRequestFilter
    * No requests were filtered when self-description was exposed by config

## V1.0.0-alpha

This version is compatible to **Eclipse Dataspace Connector v0.0.1-Milestone 6**

**New Features**

* Custom AuthenticationRequestFilter
    * Requests to the extension are now intercepted by a request filter
    * Requests to all endpoints except `/api/selfDescription` require authentication
    * The authentication method is defined within the launcher's build file. In the example launcher, a mocked identity
      and access management service is used: `_org.eclipse.edc:iam-mock_`
    * Available authentication methods are defined by the EDC. For further information see the EDCs
      AuthenticationServices and AuthenticationRequestFilters.

* Configuration value "exposeSelfDescription"
    * Set to _False_ to not expose the extension's self-descriptions
    * Default value is _True_

**Bugfixes**

* FaaastServiceManager
    * When booting with FA³ST config
        * Only boot up if HTTP endpoint is provided
* Forwarding requests to an AAS service
    * Forwarding POST/DELETE requests used HTTP PUT method
* Handling of HTTP status code from AAS service
    * Incorrect return values were assumed on client side
* Synchronization
    * SubmodelElements were not checked by synchronization
    * Also, submodels with modified submodelElements were reloaded into the AssetIndex & ContractDefinitionStore
        * Contract agreements were no longer valid because of this
