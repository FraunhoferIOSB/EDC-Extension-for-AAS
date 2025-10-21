plugins {
    jacoco
}

dependencies {
    implementation(project(":extensions:common:aas-lib"))

    implementation(libs.edc.data.plane.spi)
    implementation(libs.edc.http.lib)

    testImplementation(libs.edc.junit)
    testImplementation(libs.aas4j.model)

    testImplementation(libs.wiremock)
    testImplementation(libs.jupiter)
    testImplementation(libs.mockito)
    testImplementation("org.mock-server:mockserver-junit-jupiter:5.15.0")
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test { useJUnitPlatform() }
tasks.jacocoTestReport { dependsOn(tasks.test) }
