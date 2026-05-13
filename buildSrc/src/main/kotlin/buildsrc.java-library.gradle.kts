import com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep

plugins {
    id("com.diffplug.spotless")
    `java-library`
}

spotless {
    format("misc") {
        target("*.md", "*.yms", "*.json", "*.xml", "*.yaml", "*.yml", "*.properties", "*.gradle", "*.kts")

        trimTrailingWhitespace()
        endWithNewline()
    }

    java {
        target("src/*/java/**/*.java")
        eclipse("4.32").configFile(rootProject.file("misc/checkstyle/formatter.xml"))

        importOrder()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

project.plugins.apply("java-library")
project.plugins.apply("jacoco")

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
}

tasks.named("check") {
    dependsOn("spotlessCheck")
}
