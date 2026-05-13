plugins {
    id("buildsrc.java-library")
}

dependencies {
    implementation(libs.edc.auth.spi)
    implementation(libs.edc.jersey.core) // WebService
    implementation(libs.edc.jetty.core) // WebService
    implementation(libs.edc.api.core) // ApiAuthenticationRegistry

    testImplementation(testFixtures(project(":extensions:common:aas-lib")))
}


