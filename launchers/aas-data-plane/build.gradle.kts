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
    runtimeOnly(libs.edc.dataplane.base.bom)
    runtimeOnly(libs.edc.configuration.filesystem)  // read config from files
    runtimeOnly(libs.edc.auth.tokenbased)
    runtimeOnly(libs.edc.vault.hashicorp)
    runtimeOnly(libs.edc.api.core) // ApiAuthenticationRegistry
    runtimeOnly(libs.edc.auth.configuration)

    runtimeOnly(project(":extensions:data-plane:data-plane-aas"))
}
