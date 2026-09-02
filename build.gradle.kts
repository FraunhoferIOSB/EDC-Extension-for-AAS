plugins {
    `java-library`
    `maven-publish`
    `jacoco-report-aggregation`
    `java-test-fixtures`
}

val testProvider = tasks.register("testSystemProvider", Exec::class) {
    description = "Execute the provider system test"
    commandLine("${rootProject.projectDir}/system-tests/provider/test.sh")
}

val testAasDataPlane = tasks.register("testSystemAasDataPlane", Exec::class) {
    description = "Execute the AAS data plane system test"
    commandLine("${rootProject.projectDir}/system-tests/aas-data-plane/test.sh")
    dependsOn(testProvider) // Wait for the provider test to finish
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
