dependencies {
    implementation(project(":extensions:common:aas-lib")) // AuthenticationMethod

    implementation(libs.edc.asset.spi)
    implementation(libs.edc.contract.spi)

    implementation(libs.edc.transform.lib)
    implementation(libs.edc.control.plane.transform)
    implementation(libs.edc.json.ld)
    implementation(libs.edc.runtime.core)
    implementation(libs.edc.connector.core)

    testImplementation(testFixtures(project(":extensions:common:aas-lib")))
    testImplementation(project(":extensions:common:data-plane-aas-spi"))
    testImplementation(project(":extensions:common:validator:validator-data-address-aas-data"))

    testImplementation(libs.edc.http.lib) // EdcHttpClientImpl
    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.json.lib)
    testImplementation(libs.edc.json.ld.lib)
    testImplementation(libs.mockito)

    testImplementation(libs.wiremock)
    testImplementation(libs.jupiter)
    testImplementation(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}