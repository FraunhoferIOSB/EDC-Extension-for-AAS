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
    runtimeOnly(project(":extensions:control-plane:client"))
    runtimeOnly(libs.edc.management.api.configuration) // auth for mgmt api
    runtimeOnly(libs.edc.configuration.filesystem)
    runtimeOnly(libs.edc.auth.tokenbased)
    runtimeOnly(libs.edc.auth.configuration)
    runtimeOnly(libs.edc.vault.hashicorp)
    runtimeOnly(libs.edc.iam.mock) // DefaultParticipantIdExtraction
    runtimeOnly(libs.edc.api.core) // ApiAuthenticationRegistry
    runtimeOnly(project(":launchers:aas-data-plane")) { // data-plane (including AAS data-plane)
        // This requires edc.dpf.selector.url which we don't need here as the dataplane is running in the same instance
        // as the control-plane.
        exclude("org.eclipse.edc", "data-plane-selector-client")
    }
}