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

    testRuntimeOnly(libs.edc.controlplane.base.bom) // EDC control-plane runtime for embedded-runtime integration tests
    testRuntimeOnly(libs.edc.auth.tokenbased) // x-api-key authentication for the default api context
    testRuntimeOnly(libs.edc.auth.configuration) // binds web.http.default.auth.* config to the api contexts
    testRuntimeOnly(libs.edc.iam.mock) // IdentityService, AudienceResolver and DefaultParticipantIdExtractionFunction for the auto-loaded dsp/control-plane extensions
}
