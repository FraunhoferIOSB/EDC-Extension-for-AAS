plugins {
    jacoco
}

dependencies {
    implementation(project(":extensions:common:aas-lib"))
    implementation(project(":extensions:common:data-plane-aas-spi"))

    implementation(libs.edc.data.plane.spi)
    implementation(libs.edc.http.lib)

    testImplementation(testFixtures(project(":extensions:common:aas-lib")))

    testImplementation(libs.aas4j.model)
}

tasks.test { useJUnitPlatform() }
tasks.jacocoTestReport { dependsOn(tasks.test) }
