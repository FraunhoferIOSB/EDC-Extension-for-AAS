include("extensions:common:aas-lib")
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