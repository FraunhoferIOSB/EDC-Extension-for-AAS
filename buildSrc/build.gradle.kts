plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(libs.shadow.gradle.plugin)
    implementation(libs.docker.gradle.plugin)
    implementation(libs.spotless.gradle.plugin)
}
