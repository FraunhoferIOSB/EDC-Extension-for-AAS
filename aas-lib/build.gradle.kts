plugins {
    id("java")
}

val edcVersion: String by project
val aas4jVersion: String by project
val junitPlatformLauncherVersion: String by project

repositories {
    mavenCentral()
}

dependencies {
    implementation("${group}:asset-spi:${edcVersion}")
    implementation("${group}:data-plane-http-spi:${edcVersion}")
    implementation("org.eclipse.digitaltwin.aas4j:aas4j-model:${aas4jVersion}")

    testImplementation("$group:junit:$edcVersion")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:$junitPlatformLauncherVersion")

}

tasks.test {
    useJUnitPlatform()
}