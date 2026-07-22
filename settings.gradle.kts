include("extensions:common:aas-lib")
include("extensions:common:constants")
include("extensions:common:data-plane-aas-spi")
include("extensions:common:json-ld:json-ld-fx")
include("extensions:common:json-ld:json-ld-aas")
include("extensions:common:data-plane-aas-spi")
include("extensions:common:util:policy-util")
include("extensions:common:validator:validator-data-address-aas-data")
include("extensions:control-plane:aas:dtr")
include("extensions:control-plane:client")
include("extensions:control-plane:codec")
include("extensions:control-plane:edc-extension4aas")
include("extensions:control-plane:public-api-management")
include("extensions:data-plane:data-plane-aas")
include("extensions:edc-connector-client")
include("extensions:repository-aas")

include("launchers:aas-data-plane")
include("launchers:consumer")
include("launchers:provider")
include("launchers:standalone")
include("launchers:tractus-x")
include("samples:hercules:standalone-hercules")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        id("com.gradleup.shadow") version "9.5.1"
        id("com.bmuschko.docker-remote-api") version "10.0.0"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://central.sonatype.com/repository/maven-snapshots")
    }
}
