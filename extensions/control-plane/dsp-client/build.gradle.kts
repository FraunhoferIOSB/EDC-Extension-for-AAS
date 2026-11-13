plugins {
    id("buildsrc.java-library")
}

dependencies {
    implementation(libs.edc.web.spi) // Controller
    implementation(libs.edc.control.plane.contract) // Observe contract negotiations
    implementation(libs.edc.control.plane.transfer) // Observe transfer processes
    implementation(libs.edc.dsp.catalog.transform2025) // JSON to Catalog transformers
    implementation(libs.edc.json.ld.lib) // JsonLD expansion

    testImplementation(testFixtures(project(":extensions:common:aas-lib")))

    testImplementation(libs.aas4j.model)
    testImplementation(libs.edc.data.plane.http.spi)
}
