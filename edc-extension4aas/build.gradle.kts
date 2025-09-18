plugins {
    `java-library`
    jacoco
}

val javaVersion: String by project
val faaastVersion: String by project
val edcVersion: String by project
val mockitoVersion: String by project
val mockserverVersion: String by project
val jerseyVersion: String by project
val junitPlatformLauncherVersion: String by project

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
}

dependencies {
    implementation(project(":public-api-management"))
    implementation(project(":aas-lib"))

    implementation("de.fraunhofer.iosb.ilt.faaast.service:starter:$faaastVersion")
    implementation("${group}:http-lib:${edcVersion}")
    implementation("${group}:data-plane-http-spi:${edcVersion}") // HTTPDataAddress
    implementation("$group:asset-api:$edcVersion")
    implementation("${group}:json-ld-spi:${edcVersion}") // Add aas NS to context
    implementation("${group}:policy-engine-lib:${edcVersion}")

    testImplementation(testFixtures(project(":aas-lib")))

    testImplementation("$group:junit:$edcVersion")
    testImplementation("org.glassfish.jersey.core:jersey-common:$jerseyVersion")
    testImplementation("org.mockito:mockito-core:${mockitoVersion}")
    testImplementation("org.mock-server:mockserver-junit-jupiter:$mockserverVersion")
    testImplementation("org.mock-server:mockserver-netty:$mockserverVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:$junitPlatformLauncherVersion")
}

repositories {
    mavenCentral()
}

tasks.test { useJUnitPlatform() }
tasks.jacocoTestReport { dependsOn(tasks.test) }