plugins {
    jacoco
}

dependencies {
    implementation(libs.edc.boot.spi)
    implementation(project(":extensions:common:aas-lib")) // Ping Host for availability


    implementation(libs.fa3st.client)
    implementation(libs.fa3st.starter)
    implementation(libs.edc.util.lib)

    testImplementation(testFixtures(project(":extensions:common:aas-lib")))

    testImplementation(libs.edc.junit)
    testImplementation(libs.mockito)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.jupiter)
    testImplementation(libs.mockito)

}

tasks.test { useJUnitPlatform() }
tasks.jacocoTestReport { dependsOn(tasks.test) }