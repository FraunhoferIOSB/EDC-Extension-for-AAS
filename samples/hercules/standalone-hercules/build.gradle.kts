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
    implementation(project(":extensions:control-plane:aas:dtr"))
    runtimeOnly(project(":launchers:standalone"))

    runtimeOnly(project(":extensions:common:json-ld:json-ld-fx"))
    runtimeOnly("org.eclipse.tractusx.edc:json-ld-cx:0.12.1")
}
