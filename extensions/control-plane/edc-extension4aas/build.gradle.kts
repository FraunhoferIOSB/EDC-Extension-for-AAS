plugins {
    jacoco
}

dependencies {
    implementation(project(":extensions:common:constants"))
    implementation(project(":extensions:common:data-plane-aas-spi"))
    implementation(project(":extensions:control-plane:public-api-management"))
    implementation(project(":extensions:common:aas-lib"))

    implementation(libs.fa3st.starter)
    implementation(libs.edc.asset.spi)
    implementation(libs.edc.contract.spi)
    implementation(libs.edc.data.plane.http.spi)
    implementation(libs.edc.http.lib)
    implementation(libs.edc.json.ld.spi)

    testImplementation(testFixtures(project(":extensions:common:aas-lib")))

    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.control.plane.core)
    testImplementation(libs.edc.query.lib)
    testImplementation(libs.jersey.common)
    testImplementation(libs.mockito)
    testImplementation("org.mock-server:mockserver-junit-jupiter:5.15.0") // TODO replace w wiremock
    testImplementation("org.mock-server:mockserver-netty:5.15.0") // TODO replace w wiremock
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test { useJUnitPlatform() }
tasks.jacocoTestReport { dependsOn(tasks.test) }