# Changelog

## Current development version 

**New Features**

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