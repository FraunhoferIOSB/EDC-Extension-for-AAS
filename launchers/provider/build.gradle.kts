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
    id("buildsrc.java-library")
    id("buildsrc.application")
}

dependencies {
    implementation(libs.edc.boot.lib)

    runtimeOnly(libs.edc.controlplane.base.bom)
    runtimeOnly(project(":extensions:control-plane:edc-extension4aas"))
    runtimeOnly(project(":extensions:control-plane:dsp-client"))

    runtimeOnly(libs.edc.iam.mock) // Only for participant ID extraction function

    runtimeOnly(libs.edc.dataplane.base.bom) // Transfer type HttpData-{PULL,PUSH}
    runtimeOnly(libs.tx.edc.dataplane.proxy.http)
    runtimeOnly(libs.tx.edc.dataplane.public.api.v2)

    runtimeOnly(project(":launchers:aas-data-plane")) {
        // This requires edc.dpf.selector.url which we don't need here as the dataplane is running in the same instance
        // as the control-plane.
        exclude("org.eclipse.edc", "data-plane-selector-client")
        exclude("org.eclipse.edc", "vault-hashicorp")
    }
}

configurations.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("org.eclipse.edc:data-plane-util"))
            .using(module(libs.tx.edc.dataplane.util.get().toString()))
            .because("Tractus-X ships a binary fork of data-plane-util (AsyncResponseCallback with statusCode()/proxyHeaders()) required by DataPlanePublicApiV2Controller; keeping both on the classpath/shadowJar causes duplicate classes and NoSuchMethodError")
    }
}
