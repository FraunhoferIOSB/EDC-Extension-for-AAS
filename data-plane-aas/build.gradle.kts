plugins {
    `java-library`
    jacoco
}

val edcVersion: String by project
val aas4jVersion: String by project
val faaastVersion: String by project
val jupiterVersion: String by project

repositories { mavenCentral() }

dependencies {
    implementation("$group:data-plane-spi:${edcVersion}")
    implementation("$group:http-spi:${edcVersion}")
    implementation("org.eclipse.digitaltwin.aas4j:aas4j-model:${aas4jVersion}")

    testImplementation("$group:junit:$edcVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testImplementation("de.fraunhofer.iosb.ilt.faaast.service:starter:${faaastVersion}")
}

tasks.test { useJUnitPlatform() }
tasks.jacocoTestReport { dependsOn(tasks.test) }
