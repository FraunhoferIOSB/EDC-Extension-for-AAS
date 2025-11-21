/*
 * Copyright (c) 2021 Fraunhofer IOSB, eine rechtlich nicht selbstaendige
 * Einrichtung der Fraunhofer-Gesellschaft zur Foerderung der angewandten
 * Forschung e.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fraunhofer.iosb.app.controller.dto;

import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.model.config.impl.faaast.FaaastRepositoryConfig;

import java.util.List;
import java.util.Objects;


/**
 * DTO containing information to start a local AAS repository.
 *
 * @param modelPath Path to the AAS environment (optional, default: empty environment).
 * @param port Port to use for communications with AAS repository (optional, default: random available port).
 * @param configPath Path to configuration for the AAS server (optional, default: no configuration/defaults).
 * @param policyBindings List of {@link PolicyBinding}. If defined, only elements referred by the policyBindings are registered (optional, default: no custom
 *         PolicyBindings, register all elements).
 */
public record LocalRepositoryDTO(String modelPath, Integer port, String configPath, List<PolicyBinding> policyBindings) {
    public LocalRepositoryDTO {
        policyBindings = Objects.requireNonNullElse(policyBindings, List.of());
    }


    public LocalRepositoryDTO(String modelPath, Integer port, String configPath) {
        this(modelPath, port, configPath, List.of());
    }


    public FaaastRepositoryConfig asConfig() {
        return FaaastRepositoryConfig.Builder.newInstance()
                .model(modelPath())
                .port(port())
                .configPath(configPath())
                .policyBindings(policyBindings())
                .build();
    }
}
