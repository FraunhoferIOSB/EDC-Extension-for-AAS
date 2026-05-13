plugins {
    checkstyle
    `java-library`
    `maven-publish`
    `jacoco-report-aggregation`
    `java-test-fixtures`
    alias(libs.plugins.edc.build)
}

tasks.check {
    dependsOn(tasks.named<JacocoReport>("testCodeCoverageReport"))
}
