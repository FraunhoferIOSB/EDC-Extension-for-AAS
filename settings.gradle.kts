// include the extensions in the build process
include("aas-lib")
include("client")
include("data-plane-aas")
include("edc-extension4aas")
include("public-api-management")
include("edc-connector-client")

// include the launcher in the build process
include("example")
// tractus-x example
include("example:tractus-x:control-plane")
include("example:tractus-x:data-plane")
include("example:standalone:extension")
include("example:standalone:control-plane")