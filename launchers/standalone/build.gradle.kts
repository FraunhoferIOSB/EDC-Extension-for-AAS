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
    runtimeOnly(libs.edc.boot)
    runtimeOnly(libs.edc.configuration.filesystem)

    runtimeOnly(project(":extensions:control-plane:edc-extension4aas"))
    runtimeOnly(project(":extensions:edc-connector-client"))

    runtimeOnly(libs.edc.iam.mock)
    runtimeOnly(libs.edc.participant.context.config.core) // IAM Mock and Vault need participant context config
    runtimeOnly(libs.edc.participant.context.single.core) // AAS extension needs this to resolve participant id for policies

    runtimeOnly(libs.edc.oauth2.client) // If using FA³ST security / AAS repository with oidc, an oauth client is needed

    runtimeOnly(libs.edc.auth.tokenbased)
    runtimeOnly(libs.edc.auth.configuration)
    runtimeOnly(libs.edc.vault.hashicorp)
}
