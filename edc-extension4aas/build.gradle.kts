plugins {
    `java-library`
    jacoco
    id("io.github.goooler.shadow") version "8.1.8"
}

val javaVersion: String by project
val faaastVersion: String by project
val edcVersion: String by project
val rsApi: String by project
val mockitoVersion: String by project
val mockserverVersion: String by project
val jerseyVersion: String by project

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
}

dependencies {
    implementation(project(":public-api-management"))
    implementation(project(":data-plane-aas"))

    implementation("de.fraunhofer.iosb.ilt.faaast.service:starter:${faaastVersion}")
    implementation("$group:http-spi:${edcVersion}")
    implementation("$group:asset-api:$edcVersion")
    implementation("$group:web-spi:$edcVersion")

    testImplementation("$group:junit:$edcVersion")
    testImplementation("org.glassfish.jersey.core:jersey-common:$jerseyVersion")
    testImplementation("org.mockito:mockito-core:${mockitoVersion}")
    testImplementation("org.mock-server:mockserver-junit-jupiter:${mockserverVersion}")
    testImplementation("org.mock-server:mockserver-netty:${mockserverVersion}")
}

repositories {
    mavenCentral()
}

tasks.shadowJar {
    isZip64 = true
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("extension.jar")
    archiveClassifier.set("shadow")
    configurations = listOf(project.configurations.runtimeClasspath.get())
    relocate("org.eclipse.jetty", "shaded.jetty11") {
        include("org.eclipse.jetty.**")
    }
    from(project.configurations.runtimeClasspath.get().map { if (it.isDirectory) it else project.zipTree(it) })
}

tasks.test { useJUnitPlatform() }
tasks.jacocoTestReport { dependsOn(tasks.test) }