plugins {
    `java-library`
    jacoco
}

val javaVersion: String by project
val edcVersion: String by project
val rsApi: String by project
val mockitoVersion: String by project
val mockserverVersion: String by project

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
}

dependencies {
    // See this project's README.MD for explanations
    implementation("$group:auth-spi:$edcVersion")

    testImplementation("$group:junit:$edcVersion")
    testImplementation("org.glassfish.jersey.core:jersey-common:3.1.8")
    testImplementation("org.mockito:mockito-core:${mockitoVersion}")
    testImplementation("org.mock-server:mockserver-junit-jupiter:${mockserverVersion}")
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
