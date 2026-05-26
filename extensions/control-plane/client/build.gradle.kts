plugins {
    id("buildsrc.java-library")
}

dependencies {
    implementation(project(":extensions:control-plane:public-api-management"))

    implementation(libs.edc.connector.core) // PolicyService
    implementation(libs.edc.control.plane.contract) // Observe contract negotiations
    implementation(libs.edc.control.plane.transfer) // Observe contract negotiations
    implementation(libs.edc.control.plane.transform) // Type transformers
    implementation(libs.edc.data.plane.http.spi) // EDC HttpDataAddress
    implementation(libs.edc.dsp.catalog.http.dispatcher) // DSP HTTP constant
    implementation(libs.edc.federated.catalog.core) // Transformers
    implementation(libs.edc.federated.catalog.core2025) // JsonObjectToCatalogTransformer
    implementation(libs.edc.json.ld.lib) // JsonLD expansion
    implementation(libs.edc.dsp2025) // DSP 2025 constants (protocol name)

    testImplementation(testFixtures(project(":extensions:common:aas-lib")))

    testImplementation(libs.aas4j.model)
}
