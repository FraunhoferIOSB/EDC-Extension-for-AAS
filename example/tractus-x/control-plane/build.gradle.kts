plugins {
    `java-library`
    id("application")
    id("io.github.goooler.shadow") version "8.1.8"
}

val edcVersion: String by project

dependencies {
    // tractus-x control-plane
    runtimeOnly("org.eclipse.tractusx.edc:edc-controlplane-base:0.10.0") {
        //exclude("org.eclipse.tractusx.edc", "tx-iatp-sts-dim")
        //exclude("org.eclipse.tractusx.edc", "tx-dcp-sts-dim")
    }
    // our extension
    implementation(project(":edc-extension4aas"))
    // optionally: automatic contract negotiation
    // implementation(project(":client"))
    // Identity and Access Management MOCK -> only for testing
    runtimeOnly("$group:iam-mock:$edcVersion")
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
