include("extensions:common:aas-lib")
include("extensions:common:constants")
include("extensions:common:data-plane-aas-spi")
include("extensions:common:validator:validator-data-address-aas-data")
include("extensions:control-plane:client")
include("extensions:control-plane:edc-extension4aas")
include("extensions:control-plane:public-api-management")
include("extensions:data-plane:data-plane-aas")
include("extensions:edc-connector-client")

include("launchers:aas-data-plane")
include("launchers:consumer")
include("launchers:provider")
include("launchers:standalone")
include("launchers:tractus-x")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}