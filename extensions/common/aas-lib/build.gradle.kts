plugins {
    `java-test-fixtures`
}

dependencies {
    implementation(project(":extensions:common:constants"))
    implementation(project(":extensions:common:data-plane-aas-spi"))
    runtimeOnly(project(":extensions:common:validator:validator-data-address-aas-data"))
    implementation(libs.edc.asset.spi)
    implementation(libs.fa3st.model) // ReferenceHelper

    testImplementation(libs.edc.junit)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    testFixturesRuntimeOnly(libs.junit.platform.launcher)
    testFixturesImplementation(libs.jupiter)
    testFixturesImplementation(project(":extensions:common:constants"))
    testFixturesImplementation(project(":extensions:common:data-plane-aas-spi"))
    testFixturesImplementation(platform(libs.junit.bom))
    testFixturesImplementation(libs.edc.junit)
    testFixturesImplementation(libs.commons.io)
    testFixturesImplementation(libs.aas4j.dataformat.json)
}

tasks.test {
    useJUnitPlatform()
}