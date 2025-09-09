plugins {
    `java-library`
}

val edcVersion: String by project
val javaVersion: String by project
val edcClientVersion: String by project
val junitPlatformLauncherVersion: String by project
val mockitoVersion: String by project
val jerseyVersion: String by project
val jupiterVersion: String by project

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
}

dependencies {
    implementation("$group:asset-spi:$edcVersion")
    implementation("$group:http-spi:$edcVersion")
    implementation("$group:policy-spi:$edcVersion")
    implementation("$group:contract-spi:$edcVersion")

    implementation("$group:transform-lib:$edcVersion")
    implementation("$group:control-plane-transform:$edcVersion")
    implementation("$group:json-ld:$edcVersion")
    implementation("${group}:runtime-core:${edcVersion}")
    implementation("${group}:connector-core:${edcVersion}")

    testImplementation("${group}:http-lib:${edcVersion}") // EdcHttpClientImpl
    testImplementation("org.mockito:mockito-core:${mockitoVersion}")

    testImplementation("com.github.tomakehurst:wiremock:3.0.1")
    testImplementation("org.junit.jupiter:junit-jupiter:${jupiterVersion}")
    testImplementation("org.junit.platform:junit-platform-launcher")
}

repositories {
    mavenCentral()
}

tasks.test {
    useJUnitPlatform()
}