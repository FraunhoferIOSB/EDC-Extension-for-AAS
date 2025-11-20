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
    runtimeOnly(libs.edc.controlplane.base.bom)
    runtimeOnly(project(":extensions:control-plane:edc-extension4aas"))
    runtimeOnly(project(":extensions:control-plane:client"))

    runtimeOnly(libs.edc.vault.hashicorp)
    runtimeOnly(libs.edc.iam.mock) // Only for participant ID extraction function

    runtimeOnly(project(":launchers:aas-data-plane")) {
        // This requires edc.dpf.selector.url which we don't need here as the dataplane is running in the same instance
        // as the control-plane.
        exclude("org.eclipse.edc", "data-plane-selector-client")
    }
}
