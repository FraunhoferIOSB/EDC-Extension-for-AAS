# Changelog

## Current development version 

**New Features**

* Support of **Eclipse Dataspace Connection v0.0.1-Milestone 7**

**Bugfixes**

## V1.0.0-alpha
This version is compatible to **Eclipse Dataspace Connection v0.0.1-Milestone 6**

**New Features**

* Custom AuthenticationRequestFilter
    * Requests to the extension are now intercepted by a request filter
    * Requests to all endpoints except `/api/selfDescription` require authentication
    * The authentication method is defined within the launcher's build file. In the example launcher, a mocked identity and access management service is used: `_org.eclipse.edc:iam-mock_`
    * Available authentication methods are defined by the EDC. For further information see the EDC's AuthenticationServices and AuthenticationRequestFilters.

* Configuration value "exposeSelfDescription"
    * Set to _False_ to not expose the extension's self descriptions
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
