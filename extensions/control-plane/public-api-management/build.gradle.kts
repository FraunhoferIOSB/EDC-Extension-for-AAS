plugins {
    jacoco
}

dependencies {
    implementation(libs.edc.auth.spi)
    implementation(libs.edc.jersey.core) // WebService
    implementation(libs.edc.jetty.core) // WebService
    implementation(libs.edc.api.core) // ApiAuthenticationRegistry

    testImplementation(libs.edc.junit)
    testImplementation(libs.jersey.common)
}

tasks.test {
    useJUnitPlatform()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
}
