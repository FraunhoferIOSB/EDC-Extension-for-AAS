plugins {
    `java-test-fixtures`
}

dependencies {
    implementation(project(":extensions:common:validator:validator-data-address-aas-data"))
    implementation(libs.edc.asset.spi)
    implementation(libs.edc.data.plane.http.spi)
    implementation(libs.aas4j.model)
    implementation(libs.aas4j.dataformat.json)
    implementation(libs.fa3st.model) // ReferenceHelper

    testImplementation(libs.edc.junit)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    testFixturesRuntimeOnly(libs.junit.platform.launcher)
    testFixturesImplementation(libs.jupiter)
    testFixturesImplementation(platform(libs.junit.bom))
    testFixturesImplementation(libs.edc.junit)
    testFixturesImplementation(libs.commons.io)
    testFixturesImplementation(libs.aas4j.dataformat.json)
}

tasks.test {
    useJUnitPlatform()
}