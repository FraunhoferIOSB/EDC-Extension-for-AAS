plugins {
    `java-library`
    id("application")
    id("io.github.goooler.shadow") version "8.1.8"
}

dependencies {
    runtimeOnly("org.eclipse.tractusx.edc:edc-dataplane-base:0.10.0")
    implementation(project(":data-plane-aas"))
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.shadowJar {
    isZip64 = true
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("dataspace-connector.jar")
    from(project.configurations.runtimeClasspath.get().map { if (it.isDirectory) it else project.zipTree(it) })
}

repositories {
    mavenCentral()
}
