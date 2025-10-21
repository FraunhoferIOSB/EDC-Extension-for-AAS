plugins {
    jacoco
}

dependencies {
    implementation(project(":extensions:control-plane:public-api-management"))


    implementation(libs.edc.connector.core) // PolicyService
    implementation(libs.edc.control.plane.contract) // Observe contract negotiations
    implementation(libs.edc.control.plane.transform) // Type transformers
    implementation(libs.edc.data.plane.http.spi) // EDC HttpDataAddress
    implementation(libs.edc.dsp.catalog.http.dispatcher) // DSP HTTP constant
    implementation(libs.edc.federated.catalog.core) // Transformers
    implementation(libs.edc.federated.catalog.core2025) // JsonObjectToCatalogTransformer
    implementation(libs.edc.json.ld.lib) // JsonLD expansion

    testImplementation(libs.edc.junit)
    testImplementation(libs.jersey.common)
    testImplementation(libs.mockito)
    testImplementation("org.mock-server:mockserver-junit-jupiter:5.15.0")
    testImplementation(libs.aas4j.model)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
}
