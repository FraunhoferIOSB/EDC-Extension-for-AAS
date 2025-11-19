plugins {
    jacoco
}

dependencies {
    implementation(libs.edc.boot.spi)
    implementation(project(":extensions:common:aas-lib")) // Ping Host for availability

    implementation(libs.fa3st.client)
    implementation(libs.fa3st.starter)
    implementation(libs.edc.util.lib)

    testImplementation(testFixtures(project(":extensions:common:aas-lib")))
}

tasks.test { useJUnitPlatform() }
tasks.jacocoTestReport { dependsOn(tasks.test) }

repositories {
    mavenCentral()
    maven { url = uri("https://central.sonatype.com/repository/maven-snapshots/") }
}
