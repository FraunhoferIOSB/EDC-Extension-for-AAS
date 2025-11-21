plugins {
    id("java")
}

group = "de.fraunhofer.iosb"
version = "2.3.0"

dependencies {
    implementation(project(":extensions:common:constants"))
    implementation(libs.edc.data.plane.http.spi) // BASE_URL

    implementation(libs.aas4j.model) // Reference, KeyTypes
    implementation(libs.fa3st.model) // ReferenceHelper
    implementation(libs.edc.core.spi)
    implementation(libs.aas4j.dataformat.json) // Json (De)Serializer

    testImplementation(testFixtures(project(":extensions:common:aas-lib")))
}

tasks.test {
    useJUnitPlatform()
}
