plugins {
    `java-library`
    `maven-publish`
    `jacoco-report-aggregation`
    `java-test-fixtures`
    alias(libs.plugins.edc.build)
}

val edcBuildId = libs.plugins.edc.build.get().pluginId

allprojects {
    apply(plugin = edcBuildId)

    configure<CheckstyleExtension> {
        configFile = rootProject.file("misc/checkstyle/checkstyle.xml")
        configDirectory.set(rootProject.file("misc/checkstyle"))
    }

    // Remove mavenLocal added by edc-build's RepositoriesConvention to prevent
    // stale local POMs (e.g. FA³ST snapshots with unresolved properties) from
    // overriding remote snapshot resolution
    repositories.findByName("MavenLocal")?.let { repositories.remove(it) }
}

tasks.check {
    dependsOn(tasks.named<JacocoReport>("testCodeCoverageReport"))
}
