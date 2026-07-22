plugins {
    id("buildsrc.java-library")
}


dependencies {
    implementation(project(":extensions:control-plane:public-api-management"))
    implementation(project(":extensions:common:aas-lib"))
    implementation(project(":extensions:common:util:policy-util"))
    implementation(project(":extensions:repository-aas"))
    implementation(project(":extensions:control-plane:codec"))
    implementation(project(":extensions:common:json-ld:json-ld-aas"))

    implementation(libs.fa3st.model) // ReferenceHelper
    implementation(libs.aas4j.model) // AasUtils
    implementation(libs.aas4j.dataformat.json) // AAS Serialization

    implementation(libs.edc.asset.spi)
    implementation(libs.edc.contract.spi)
    implementation(libs.edc.control.plane.transform)
    implementation(libs.edc.data.plane.http.spi)
    implementation(libs.edc.http.lib)
    implementation(libs.edc.oauth2.spi)
    implementation(libs.edc.util.lib)
    implementation(libs.edc.participant.context.single.spi) // SingleParticipantResolver

    testImplementation(testFixtures(project(":extensions:common:aas-lib")))

    testImplementation(libs.edc.control.plane.core)
    testImplementation(libs.edc.query.lib)
    testImplementation(libs.fa3st.dataformat.json)
}
