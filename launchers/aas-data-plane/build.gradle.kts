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
    id("buildsrc.application")
}

dependencies {
    runtimeOnly(libs.edc.dataplane.base.bom)

    runtimeOnly(libs.edc.auth.tokenbased)
    runtimeOnly(libs.edc.vault.hashicorp)

    runtimeOnly(libs.edc.participant.context.config.core) // ParticipantContextConfig
    runtimeOnly(libs.edc.participant.context.core) // Vault needs participant context config

    runtimeOnly(libs.edc.api.core) // ApiAuthenticationRegistry
    runtimeOnly(libs.edc.auth.configuration)

    runtimeOnly(project(":extensions:data-plane:data-plane-aas"))
}
