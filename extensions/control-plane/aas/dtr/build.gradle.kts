plugins {
    id("buildsrc.java-library")
}


dependencies {
    runtimeOnly("org.eclipse.tractusx.edc:json-ld-cx:0.12.1")

    //implementation(project(":extensions:common:aas-lib"))
    implementation(project(":extensions:repository-aas"))
    implementation(project(":extensions:control-plane:codec"))
    implementation(project(":extensions:common:util:policy-util"))

    implementation(libs.edc.asset.spi)
    implementation(libs.edc.contract.spi)
    implementation(libs.edc.data.plane.http.spi)
    implementation(libs.edc.util.lib)
    implementation(libs.edc.participant.context.single.spi) // SingleParticipantResolver
}
