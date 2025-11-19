plugins {
    jacoco
}

dependencies {
    implementation(libs.edc.auth.spi)
    implementation(libs.edc.jersey.core) // WebService
    implementation(libs.edc.jetty.core) // WebService
    implementation(libs.edc.api.core) // ApiAuthenticationRegistry

    testImplementation(testFixtures(project(":extensions:common:aas-lib")))
}

tasks.test {
    useJUnitPlatform()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
}
