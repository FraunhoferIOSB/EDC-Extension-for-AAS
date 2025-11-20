plugins {
    jacoco
}


dependencies {
    implementation(project(":extensions:control-plane:public-api-management"))
    implementation(project(":extensions:common:aas-lib"))
    implementation(project(":extensions:repository-aas"))

    implementation(libs.fa3st.model) // ReferenceHelper
    implementation(libs.aas4j.model) // AasUtils
    implementation(libs.aas4j.dataformat.json) // AAS Serialization

    implementation(libs.edc.asset.spi)
    implementation(libs.edc.contract.spi)
    implementation(libs.edc.data.plane.http.spi)
    implementation(libs.edc.http.lib)
    implementation(libs.edc.json.ld.spi)
    implementation(libs.edc.util.lib)

    testImplementation(testFixtures(project(":extensions:common:aas-lib")))

    testImplementation(libs.edc.control.plane.core)
    testImplementation(libs.edc.query.lib)
    testImplementation(libs.fa3st.dataformat.json)
}

tasks.test { useJUnitPlatform() }
tasks.jacocoTestReport { dependsOn(tasks.test) }

