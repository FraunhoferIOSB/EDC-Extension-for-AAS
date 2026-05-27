plugins {
    id("buildsrc.java-library")
}

dependencies {
    implementation(project(":extensions:common:data-plane-aas-spi"))
    implementation(libs.edc.validator.spi)
    implementation(libs.edc.data.plane.http.spi) // HTTP data address "baseURL"

    testImplementation(testFixtures(project(":extensions:common:aas-lib")))

    testImplementation(libs.aas4j.model)
}
