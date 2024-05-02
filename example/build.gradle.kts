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
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

val jupiterVersion: String by project
val edcVersion: String by project

dependencies {
    implementation(project(":edc-extension4aas"))
    implementation(project(":client"))

    implementation("$group:control-plane-core:$edcVersion")
    implementation("$group:dsp:$edcVersion")

    // Identity and access management MOCK -> only for testing
    implementation("$group:iam-mock:$edcVersion")

    // Enables X-Api-Key auth
    implementation("$group:auth-tokenbased:$edcVersion")
    implementation("$group:control-api-configuration:$edcVersion")

    // Read configuration values
    implementation("$group:configuration-filesystem:$edcVersion")

    // Data transfer (read from AAS service/write to HTTP endpoint)
    implementation("$group:control-plane-api-client:$edcVersion")
    implementation("$group:data-plane-core:$edcVersion")
    implementation("$group:data-plane-http:$edcVersion")
    implementation("$group:data-plane-client:$edcVersion")
    implementation("$group:data-plane-selector-core:$edcVersion")
    implementation("$group:transfer-data-plane:$edcVersion")
}


application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    isZip64 = true
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("dataspace-connector.jar")
}

repositories {
    mavenCentral()
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.module.toString() == "com.google.inject:guice") {
            artifactSelection {
                selectArtifact(DependencyArtifact.DEFAULT_TYPE, null, null)
            }
        }
        if (requested.module.toString() == "org.yaml:snakeyaml") {
            artifactSelection {
                selectArtifact(DependencyArtifact.DEFAULT_TYPE, null, null)
            }
        }
    }
}
