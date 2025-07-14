plugins {
    `java-library`
    jacoco
}

val javaVersion: String by project
val edcVersion: String by project
val mockitoVersion: String by project
val mockserverVersion: String by project
val jerseyVersion: String by project
val aas4jVersion: String by project
val junitPlatformLauncherVersion: String by project

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
}

dependencies {
    // See this project's README.MD for explanations
    implementation(project(":public-api-management"))

    implementation("$group:connector-core:$edcVersion") // PolicyService
    implementation("$group:federated-catalog-core:$edcVersion") // Transformers
    implementation("$group:control-plane-contract:$edcVersion") // Observe contract negotiations
    implementation("$group:control-plane-transform:$edcVersion") // Type transformers
    implementation("$group:data-plane-http-spi:$edcVersion") // EDC HttpDataAddress
    implementation("$group:dsp-catalog-http-dispatcher:$edcVersion") // DSP HTTP constant
    implementation("$group:json-ld-lib:$edcVersion") // JsonLD expansion

    testImplementation("$group:junit:$edcVersion")
    testImplementation("org.glassfish.jersey.core:jersey-common:$jerseyVersion")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testImplementation("org.mock-server:mockserver-junit-jupiter:$mockserverVersion")
    testImplementation("org.eclipse.digitaltwin.aas4j:aas4j-model:$aas4jVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:$junitPlatformLauncherVersion")
}

repositories {
    mavenCentral()
}

tasks.test {
    useJUnitPlatform()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
}
