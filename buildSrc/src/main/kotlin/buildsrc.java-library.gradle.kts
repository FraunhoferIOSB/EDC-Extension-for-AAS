plugins {
    id("com.diffplug.spotless")
    id("buildsrc.publish-to-github-packages")
}

spotless {
    format("misc") {
        target("*.md", "*.yms", "*.json", "*.xml", "*.yaml", "*.yml", "*.properties", "*.gradle", "*.kts")

        trimTrailingWhitespace()
        endWithNewline()
    }

    java {
        target("src/*/java/**/*.java")
        eclipse("4.36").configFile(rootProject.file("misc/checkstyle/formatter.xml"))
        licenseHeaderFile(rootProject.file("misc/checkstyle/license-header"))

        importOrder("", "java", "\\#")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

project.plugins.apply("jacoco")
project.plugins.apply("org.eclipse.edc.edc-build")

configure<CheckstyleExtension> {
    configFile = rootProject.file("misc/checkstyle/checkstyle.xml")
    configDirectory.set(rootProject.file("misc/checkstyle"))
}

// Remove mavenLocal added by edc-build's RepositoriesConvention to prevent
// stale local POMs (e.g. FA³ST snapshots with unresolved properties) from
// overriding remote snapshot resolution
repositories.findByName("MavenLocal")?.let { repositories.remove(it) }

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
}

tasks.named("check") {
    dependsOn("spotlessCheck")
}
