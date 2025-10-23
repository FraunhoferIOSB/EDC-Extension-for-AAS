dependencies {
    implementation(project(":extensions:common:data-plane-aas-spi"))
    implementation(libs.edc.validator.spi)
    implementation(libs.edc.data.plane.http.spi) // HTTP data address "baseURL"

    testImplementation(libs.jupiter)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.jupiter)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.edc.junit)
    testImplementation(libs.aas4j.model)
}

tasks.test {
    useJUnitPlatform()
}