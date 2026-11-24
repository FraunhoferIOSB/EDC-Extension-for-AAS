plugins {
    `java-library`
    `maven-publish`
    `jacoco-report-aggregation`
    `java-test-fixtures`
}

val testProviderDockerized = tasks.register("testSystemProviderDockerized", Exec::class) {
    description = "Execute the provider system test using docker-compose instead of local gradle runs"
    commandLine("${rootProject.projectDir}/system-tests/provider-dockerized/test.sh")
}

val testAasDataPlane = tasks.register("testSystemAasDataPlane", Exec::class) {
    description = "Execute the AAS data plane system test"
    commandLine("${rootProject.projectDir}/system-tests/aas-data-plane/test.sh")
    dependsOn(testProviderDockerized) // Wait for the provider test to finish
}

val testStandalone = tasks.register("testSystemStandalone", Exec::class) {
    description = "Execute the standalone system test"
    commandLine("${rootProject.projectDir}/system-tests/standalone/test.sh")
    dependsOn(testAasDataPlane) // Wait for the AAS data plane test to finish
}

tasks.register("testSystem", Task::class) {
    description = "Run all system tests sequentially"
    dependsOn(testStandalone) // Running standalone will automatically trigger the whole chain
}

tasks.check {
    dependsOn(tasks.named<JacocoReport>("testCodeCoverageReport"))
}

allprojects {
    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
            }
        }
    }
    tasks.withType<JavaCompile>().configureEach {
        options.release.set(21)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
