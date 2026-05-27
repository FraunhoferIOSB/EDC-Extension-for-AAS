plugins {
    id("buildsrc.java-library")
}

dependencies {
    implementation(libs.edc.boot.spi)
    implementation(project(":extensions:common:aas-lib")) // Ping Host for availability

    api(libs.fa3st.client)
    implementation(libs.fa3st.starter)
    implementation(libs.edc.util.lib)

    testImplementation(testFixtures(project(":extensions:common:aas-lib")))
}
