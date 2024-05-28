# Changelog

## Current development version

Compatibility: **Eclipse Dataspace Connector v0.6.4**

**New Features**

**Bugfixes**

**Miscellaneous**

* Use Extension's HttpClient only for AAS services with self-signed HTTPS certificates

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
