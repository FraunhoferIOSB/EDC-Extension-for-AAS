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
    application
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(libs.edc.boot)
    implementation(libs.edc.configuration.filesystem)

    implementation(project(":extensions:control-plane:edc-extension4aas"))
    implementation(project(":extensions:edc-connector-client"))

    runtimeOnly(libs.edc.iam.mock)

    runtimeOnly(libs.edc.auth.tokenbased)
    runtimeOnly(libs.edc.auth.configuration)
    implementation(libs.edc.vault.hashicorp)
}