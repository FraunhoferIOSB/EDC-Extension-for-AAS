plugins {
    id("buildsrc.java-library")
}

dependencies {
    implementation(libs.edc.transform.lib)
    implementation(libs.edc.control.plane.transform)
    implementation(libs.edc.json.ld)
    implementation(libs.edc.query.lib) // CriterionOperatorRegistry

    testImplementation(testFixtures(project(":extensions:common:aas-lib")))
}

tasks.test {
    useJUnitPlatform()
}
