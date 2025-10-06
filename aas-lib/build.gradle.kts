plugins {
    `java-library`
    `java-test-fixtures`
}

val edcVersion: String by project
val aas4jVersion: String by project
val faaastVersion: String by project
val apacheCommonsVersion: String by project
val junitPlatformLauncherVersion: String by project

repositories {
    mavenCentral()
}

dependencies {
    implementation("$group:asset-spi:$edcVersion")
    implementation("$group:data-plane-http-spi:$edcVersion")
    implementation("org.eclipse.digitaltwin.aas4j:aas4j-model:$aas4jVersion")
    implementation("de.fraunhofer.iosb.ilt.faaast.service:model:$faaastVersion") // String to Reference

    testImplementation("$group:junit:$edcVersion")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:$junitPlatformLauncherVersion")

    testFixturesRuntimeOnly("org.junit.platform:junit-platform-launcher:$junitPlatformLauncherVersion")
    testFixturesImplementation("org.junit.jupiter:junit-jupiter")
    testFixturesImplementation(platform("org.junit:junit-bom:5.10.0"))
    testFixturesImplementation("$group:junit:$edcVersion")
    testFixturesImplementation("commons-io:commons-io:$apacheCommonsVersion")
    testFixturesImplementation("org.eclipse.digitaltwin.aas4j:aas4j-dataformat-json:$aas4jVersion")
}

tasks.test {
    useJUnitPlatform()
}