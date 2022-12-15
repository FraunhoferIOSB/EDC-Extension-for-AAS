# Changelog

## Current development version 

**New Features**

* Support of **Eclipse Dataspace Connection v0.0.1-Milestone 7**

* Client Service
    * Add functionality for automatically negotiating and requesting data from a provider EDC
        * On automatic negotiation, provider contractOffers are matched against a set of own contractOffers (acceptedContractOffers)
        * Provider data is sent to a specific client HTTP endpoint using a new authentication key for each transaction (derived from the agreementId)
    * Add functionality for manually
        * fetching a set of provider contractOffers for a provider asset,
        * negotiating a contract with a provider using a contractOffer, returning an agreementId or an error message,
        * getting data of a negotiated contract from a provider.

**Bugfixes**

* CustomAuthenticationRequestFilter
    * No requests were filtered when self description was exposed by config


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
