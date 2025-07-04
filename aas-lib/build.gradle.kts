plugins {
    id("java")
}

val edcVersion: String by project
val aas4jVersion: String by project

repositories {
    mavenCentral()
}

dependencies {
    implementation("$group:asset-spi:${edcVersion}")
    implementation("org.eclipse.digitaltwin.aas4j:aas4j-model:${aas4jVersion}")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.1")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}