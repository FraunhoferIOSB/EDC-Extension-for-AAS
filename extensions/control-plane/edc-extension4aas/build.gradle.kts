plugins {
    jacoco
}


dependencies {
    implementation(project(":extensions:common:constants"))
    implementation(project(":extensions:common:data-plane-aas-spi"))
    implementation(project(":extensions:control-plane:public-api-management"))
    implementation(project(":extensions:common:aas-lib"))
    implementation(project(":extensions:repository-aas"))

    implementation(libs.aas4j.model)
    implementation(libs.fa3st.starter)
    implementation(libs.edc.asset.spi)
    implementation(libs.edc.contract.spi)
    implementation(libs.edc.data.plane.http.spi)
    implementation(libs.edc.http.lib)
    implementation(libs.edc.json.ld.spi)
    implementation(libs.edc.util.lib)

    testImplementation(testFixtures(project(":extensions:common:aas-lib")))

    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.control.plane.core)
    testImplementation(libs.edc.query.lib)

}

    testRuntimeOnly(libs.junit.platform.launcher)
}

repositories {
    mavenCentral()
    mavenLocal()
}

tasks.test { useJUnitPlatform() }
tasks.jacocoTestReport { dependsOn(tasks.test) }