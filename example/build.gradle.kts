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

dependencies {
    val edcGroup = "org.eclipse.dataspaceconnector"
    val edcVersion = "0.0.1-milestone-6"

    implementation("$edcGroup:core-base:$edcVersion")
    implementation("$edcGroup:core-boot:$edcVersion")

    implementation("$edcGroup:control-plane-core:$edcVersion")

    // IDS AAS App:
    implementation(project(":edc-extension4aas"))

    // IDS stuff such as IDS messages
    implementation("$edcGroup:ids:$edcVersion") {
        exclude("$edcGroup","ids-token-validation")
    }

    // Identity and access management MOCK -> only for testing
    implementation("$edcGroup:iam-mock:$edcVersion")
    implementation("$edcGroup:auth-tokenbased:$edcVersion")

    // Read configuration values
    implementation("$edcGroup:filesystem-configuration:$edcVersion")

    // Read/write from/to http endpoints
    implementation("$edcGroup:data-management-api:$edcVersion")

    implementation("$edcGroup:data-plane-selector-client:$edcVersion")
    implementation("$edcGroup:data-plane-http:$edcVersion")
    implementation("$edcGroup:data-plane-core:$edcVersion")
    implementation("$edcGroup:data-plane-selector-core:$edcVersion")
    implementation("$edcGroup:data-plane-transfer-client:$edcVersion")

}

application {
    mainClass.set("org.eclipse.dataspaceconnector.boot.system.runtime.BaseRuntime")
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
