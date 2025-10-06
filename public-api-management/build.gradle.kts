plugins {
    `java-library`
    jacoco
}

val javaVersion: String by project
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
    // See this project's README.MD for explanations
    implementation("$group:auth-spi:$edcVersion")
    implementation("$group:jetty-core:$edcVersion") // WebService
    implementation("$group:jersey-core:$edcVersion") // WebService
    implementation("$group:api-core:$edcVersion") // ApiAuthenticationRegistry

    testImplementation("$group:junit:$edcVersion")
    testImplementation("org.glassfish.jersey.core:jersey-common:$jerseyVersion")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testImplementation("org.mock-server:mockserver-junit-jupiter:$mockserverVersion")
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
