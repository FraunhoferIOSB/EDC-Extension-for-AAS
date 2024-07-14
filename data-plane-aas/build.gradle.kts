plugins {
    `java-library`
    jacoco
}

val edcVersion: String by project
val aas4jVersion: String by project
val faaastVersion: String by project
val jupiterVersion: String by project
val mockitoVersion: String by project

repositories { mavenCentral() }

dependencies {
    implementation("$group:data-plane-spi:${edcVersion}") {
        exclude(group = "dev.failsafe", module = "failsafe")
        exclude(group = group, module = "policy-model")
    }
    implementation("$group:http-lib:${edcVersion}") {
        exclude(group = group, module = "auth-spi") // Not needed in this extension
        exclude(group = group, module = "core-spi") // Already by data-plane-spi
    }

    implementation("org.eclipse.digitaltwin.aas4j:aas4j-model:${aas4jVersion}")

    testImplementation("$group:junit:$edcVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testImplementation("de.fraunhofer.iosb.ilt.faaast.service:starter:${faaastVersion}")
    testImplementation("org.mockito:mockito-core:${mockitoVersion}")
}

tasks.test { useJUnitPlatform() }
tasks.jacocoTestReport { dependsOn(tasks.test) }
