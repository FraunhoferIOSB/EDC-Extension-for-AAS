plugins {
    id("buildsrc.java-library")
}

dependencies {
    implementation(project(":extensions:common:aas-lib"))

    implementation(libs.edc.data.plane.spi)
    implementation(libs.edc.http.lib)

    testImplementation(testFixtures(project(":extensions:common:aas-lib")))

    testImplementation(libs.aas4j.model)
}
