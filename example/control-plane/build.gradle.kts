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
    // ---- CONTROL PLANE ----
    runtimeOnly("$group:controlplane-base-bom:$edcVersion")
    implementation("$group:management-api-configuration:$edcVersion") // auth for mgmt api
    implementation("$group:configuration-filesystem:$edcVersion")  // read config from files
    implementation("$group:auth-tokenbased:$edcVersion")
    implementation("$group:vault-hashicorp:$edcVersion")
    implementation("$group:iam-mock:$edcVersion")
    implementation("$group:api-core:$edcVersion") // ApiuthenticationRegistry
    implementation("$group:auth-configuration:$edcVersion}")
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

repositories {
    mavenCentral()
}
