plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":extensions:common:constants"))
    api(project(":extensions:common:data-plane-aas-spi"))
    runtimeOnly(project(":extensions:common:validator:validator-data-address-aas-data"))
    implementation(libs.edc.asset.spi)
    implementation(libs.fa3st.model) // ReferenceHelper

    testImplementation(libs.edc.junit)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    testFixturesApi(libs.jupiter)
    testFixturesApi(project(":extensions:common:constants"))
    testFixturesApi(project(":extensions:common:data-plane-aas-spi"))
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.edc.junit)
    testFixturesApi(libs.commons.io)
    testFixturesApi(libs.aas4j.dataformat.json)
    testFixturesApi(libs.mockito)
    testFixturesApi(libs.wiremock)
    testFixturesApi(libs.jersey.common)

    testFixturesRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
