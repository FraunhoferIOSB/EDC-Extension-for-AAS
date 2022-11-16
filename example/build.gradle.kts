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
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

val jupiterVersion: String by project
val edcGroup: String by project
val edcVersion: String by project

dependencies {
    implementation("${edcGroup}:control-plane-core:${edcVersion}")

    // IDS AAS App:
    implementation(project(":edc-extension4aas"))

    // IDS stuff such as IDS messages
    implementation("${edcGroup}:ids:${edcVersion}") {
        exclude("${edcGroup}","ids-token-validation")
    }

    // Identity and access management MOCK -> only for testing
    implementation("${edcGroup}:iam-mock:${edcVersion}")
    implementation("${edcGroup}:auth-tokenbased:${edcVersion}")
    
    // Read configuration values
    implementation("${edcGroup}:configuration-filesystem:${edcVersion}")


    // Data transfer (read from AAS service/write to HTTP endpoint)
    implementation("${edcGroup}:data-plane-core:${edcVersion}")
    implementation("${edcGroup}:data-plane-http:${edcVersion}")
    implementation("${edcGroup}:data-plane-transfer-client:${edcVersion}")
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
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://maven.iais.fraunhofer.de/artifactory/eis-ids-public/")
    }
    maven {// while runtime-metamodel dependency is still a snapshot
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.module.toString() == "com.google.inject:guice") {
            artifactSelection{
                selectArtifact(DependencyArtifact.DEFAULT_TYPE, null, null)
            }
        }
        if(requested.module.toString() == "org.yaml:snakeyaml") {
            artifactSelection {
                selectArtifact(DependencyArtifact.DEFAULT_TYPE, null, null)
            }
        }
    }
}
