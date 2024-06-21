plugins {
    `java-library`
    jacoco
}

val edcVersion: String by project
val aas4jVersion: String by project


repositories { mavenCentral() }

dependencies {
    implementation("$group:data-plane-spi:${edcVersion}")
    implementation("$group:http-spi:${edcVersion}")
    implementation("org.eclipse.digitaltwin.aas4j:aas4j-model:${aas4jVersion}")
}

tasks.test { useJUnitPlatform() }