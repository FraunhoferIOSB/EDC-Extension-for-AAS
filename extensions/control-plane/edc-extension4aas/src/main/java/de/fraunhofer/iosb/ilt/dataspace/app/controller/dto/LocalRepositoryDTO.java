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
package de.fraunhofer.iosb.ilt.dataspace.app.controller.dto;

import de.fraunhofer.iosb.ilt.dataspace.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.ilt.dataspace.model.config.impl.faaast.FaaastRepositoryConfig;

import java.util.List;
import java.util.Objects;


/**
 * DTO containing information to start a local AAS repository.
 *
 * @param modelPath Path to the AAS environment (optional, default: empty environment).
 * @param port Port to use for communications with AAS repository (optional, default: random available port).
 * @param configPath Path to configuration for the AAS server (optional, default: no configuration/defaults).
 * @param policyBindings List of {@link PolicyBinding}. If defined, only elements referred by the policyBindings are
 *            registered (optional, default: no custom
 *            PolicyBindings, register all elements).
 * @param onlySubmodels If true, only register submodels. Default: true
 */
public record LocalRepositoryDTO(String modelPath, Integer port, String configPath, List<PolicyBinding> policyBindings, boolean onlySubmodels) {
    /**
     * Compact constructor normalizing the {@code policyBindings} to an empty list when {@code null}.
     */
    public LocalRepositoryDTO {
        policyBindings = Objects.requireNonNullElse(policyBindings, List.of());
    }


    /**
     * Convenience constructor delegating to the canonical constructor with {@code onlySubmodels} set to {@code true}.
     *
     * @param modelPath Path to the AAS environment (optional, default: empty environment).
     * @param port Port to use for communications with AAS repository (optional, default: random available port).
     * @param configPath Path to configuration for the AAS server (optional, default: no configuration/defaults).
     * @param policyBindings List of {@link PolicyBinding}.
     */
    public LocalRepositoryDTO(String modelPath, Integer port, String configPath, List<PolicyBinding> policyBindings) {
        this(modelPath, port, configPath, policyBindings, true);
    }


    /**
     * Convenience constructor delegating to the canonical constructor with no policy bindings and
     * {@code onlySubmodels} set to {@code true}.
     *
     * @param modelPath Path to the AAS environment (optional, default: empty environment).
     * @param port Port to use for communications with AAS repository (optional, default: random available port).
     * @param configPath Path to configuration for the AAS server (optional, default: no configuration/defaults).
     */
    public LocalRepositoryDTO(String modelPath, Integer port, String configPath) {
        this(modelPath, port, configPath, List.of(), true);
    }


    /**
     * Builds a {@link FaaastRepositoryConfig} from this DTO.
     *
     * @return The FA³ST repository configuration corresponding to this DTO.
     */
    public FaaastRepositoryConfig asConfig() {
        return FaaastRepositoryConfig.Builder.newInstance()
                .model(modelPath())
                .port(port())
                .configPath(configPath())
                .policyBindings(policyBindings())
                .onlySubmodels(onlySubmodels())
                .build();
    }
}
