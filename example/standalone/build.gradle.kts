/*
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 */

plugins {
    `java-library`
    id("application")
    id("io.github.goooler.shadow") version "8.1.8"
}

val edcVersion: String by project

dependencies {
    implementation("$group:boot:$edcVersion") // Runtime
    implementation("$group:configuration-filesystem:$edcVersion")
    // Identity and access management MOCK -> only for testing
    implementation("$group:iam-mock:$edcVersion")

    implementation(project(":edc-extension4aas"))
    // This is needed to communicate w/ the control-plane
    implementation(project(":edc-connector-client"))
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
