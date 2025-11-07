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
    runtimeOnly(libs.tx.edc.runtime.memory)
    runtimeOnly(project(":extensions:control-plane:edc-extension4aas"))
    runtimeOnly(project(":extensions:control-plane:client"))
    runtimeOnly(project(":extensions:data-plane:data-plane-aas"))
}
repositories {
    mavenCentral()
    mavenLocal()
}