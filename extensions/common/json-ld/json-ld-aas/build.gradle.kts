plugins {
    id("buildsrc.java-library")
}

dependencies {
    implementation(libs.edc.json.ld.spi)
    implementation(project(":extensions:common:aas-lib"))
}
