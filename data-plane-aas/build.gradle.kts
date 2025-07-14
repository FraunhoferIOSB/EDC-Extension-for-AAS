plugins {
    `java-library`
    jacoco
}

val edcVersion: String by project
val aas4jVersion: String by project
val faaastVersion: String by project
val jupiterVersion: String by project
val mockitoVersion: String by project
val mockserverVersion: String by project

repositories { mavenCentral() }

dependencies {
    implementation(project(":aas-lib"))

    implementation("$group:data-plane-spi:${edcVersion}") {
        exclude(group = "dev.failsafe", module = "failsafe")
        exclude(group = group, module = "policy-model")
    }
    implementation("$group:http-lib:${edcVersion}") {
        exclude(group = group, module = "auth-spi") // Not needed in this extension
        exclude(group = group, module = "core-spi") // Already by data-plane-spi
    }
    implementation("$group:asset-spi:$edcVersion")

    implementation("org.eclipse.digitaltwin.aas4j:aas4j-model:${aas4jVersion}")

    testImplementation("$group:junit:$edcVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testImplementation("de.fraunhofer.iosb.ilt.faaast.service:starter:${faaastVersion}")
    testImplementation("org.mockito:mockito-core:${mockitoVersion}")
    testImplementation("org.mock-server:mockserver-junit-jupiter:${mockserverVersion}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.3")
}

tasks.test { useJUnitPlatform() }
tasks.jacocoTestReport { dependsOn(tasks.test) }
