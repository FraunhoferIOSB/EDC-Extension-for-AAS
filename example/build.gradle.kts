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

val edcVersion: String by project

dependencies {
    // ---- CONTROL PLANE ----
    implementation("$group:control-plane-core:$edcVersion")
    implementation(project(":edc-extension4aas"))
    implementation(project(":client"))
    // Communicate status of a transfer process w/ consumer
    implementation("$group:control-plane-api:$edcVersion")
    implementation("$group:control-plane-api-client:$edcVersion")
    implementation("$group:dsp:$edcVersion") // DSP protocol for negotiation and transfer
    implementation("$group:http:$edcVersion") // WebService
    // Identity and access management MOCK -> only for testing
    implementation("$group:iam-mock:$edcVersion")
    // X-Api-Key authentication
    implementation("$group:auth-tokenbased:$edcVersion")
    // Read configuration values
    implementation("$group:configuration-filesystem:$edcVersion")
    // -----------------------

    // ---- DATA PLANE ----
    implementation("$group:data-plane-core:$edcVersion") // PipelineService
    implementation("$group:data-plane-http:$edcVersion") // Http Data Transfer
    implementation("$group:data-plane-self-registration:$edcVersion") // Register DataPlane with PipelineService factories

    implementation("$group:control-api-configuration:$edcVersion") // Needed for data-plane-self-registration
    implementation("$group:data-plane-selector-core:$edcVersion") // Needed for data-plane-self-registration
    implementation("$group:transfer-data-plane-signaling:$edcVersion") // Needed for data-plane-selector-core
    // --------------------
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
