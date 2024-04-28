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

    // Centralized auth request filter
    implementation(project(":public-api-management"))

    // See this project's README.MD for explanations
    implementation("$group:control-plane-contract:$edcVersion")
    implementation("$group:json-ld-lib:$edcVersion")
    implementation("$group:dsp-catalog-http-dispatcher:$edcVersion")
    implementation("$group:management-api:$edcVersion")
    implementation("$group:runtime-metamodel:$edcVersion")
    implementation("$group:data-plane-http-spi:$edcVersion") // HttpDataAddress
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

    testImplementation("$group:junit:$edcVersion")
    testImplementation("org.glassfish.jersey.core:jersey-common:3.1.3")
    testImplementation("org.mockito:mockito-core:${mockitoVersion}")
    testImplementation("org.mock-server:mockserver-junit-jupiter:${mockserverVersion}")
    testImplementation("org.mock-server:mockserver-netty:${mockserverVersion}")
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
