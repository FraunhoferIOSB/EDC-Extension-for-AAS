plugins {
    `java-library`
}

val edcVersion: String by project
val javaVersion: String by project
val edcClientVersion: String by project

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

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

repositories {
    mavenCentral()
}

tasks.test {
    useJUnitPlatform()
}