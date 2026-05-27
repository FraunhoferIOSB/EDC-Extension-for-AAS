plugins {
    id("buildsrc.java-library")
}

dependencies {
    implementation(project(":extensions:common:aas-lib")) // AuthenticationMethod
    implementation(project(":extensions:control-plane:codec")) // Codec

    implementation(libs.edc.asset.spi)
    implementation(libs.edc.contract.spi)
    implementation(libs.edc.runtime.core)
    implementation(libs.edc.connector.core)

    testImplementation(testFixtures(project(":extensions:common:aas-lib")))

    testImplementation(libs.edc.json.ld)
}
