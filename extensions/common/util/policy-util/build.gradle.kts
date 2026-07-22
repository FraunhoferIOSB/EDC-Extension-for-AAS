plugins {
    id("buildsrc.java-library")
}


dependencies {
    implementation(project(":extensions:common:aas-lib"))
    implementation(project(":extensions:control-plane:codec"))
    implementation(project(":extensions:common:json-ld:json-ld-aas"))

    implementation(libs.edc.policy.spi)
    implementation(libs.edc.json.ld.spi)
}
