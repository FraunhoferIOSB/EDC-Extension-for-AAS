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
    // ---- CONTROL PLANE ----
    runtimeOnly(libs.edc.controlplane.base.bom)
    runtimeOnly(libs.edc.management.api.configuration) // auth for mgmt api
    runtimeOnly(libs.edc.configuration.filesystem)
    runtimeOnly(libs.edc.auth.tokenbased)
    runtimeOnly(libs.edc.auth.configuration)
    runtimeOnly(libs.edc.vault.hashicorp)
    runtimeOnly(libs.edc.iam.mock) // DefaultParticipantIdExtraction
    runtimeOnly(libs.edc.api.core) // ApiAuthenticationRegistry
}