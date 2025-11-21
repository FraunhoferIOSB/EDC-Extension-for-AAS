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

import de.fraunhofer.iosb.aas.lib.auth.AuthenticationMethod;
import de.fraunhofer.iosb.aas.lib.auth.impl.NoAuth;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.model.context.registry.AasRegistryContext;

import java.net.URI;
import java.util.Objects;


/**
 * DTO containing information to register a remote AAS registry.
 *
 * @param url URI to use to connect to the AAS registry, including any path prefixes (e.g., /api/v3.0)
 * @param auth The authentication method used to communicate with the registry.
 */
public record AasRegistryContextDTO(URI url, AuthenticationMethod auth, String defaultAccessPolicyDefinitionId, String defaultContractPolicyDefinitionId) {
    public AasRegistryContextDTO {
        Objects.requireNonNull(url, "'url' cannot be null!");
        auth = Objects.requireNonNullElse(auth, new NoAuth());
    }


    public AasRegistryContextDTO(URI url) {
        this(url, new NoAuth());
    }


    public AasRegistryContextDTO(URI url, AuthenticationMethod auth) {
        this(url, auth, null, null);
    }


    public AasRegistryContextDTO(URI url, String defaultAccessPolicyDefinitionId, String defaultContractPolicyDefinitionId) {
        this(url, null, defaultAccessPolicyDefinitionId, defaultContractPolicyDefinitionId);
    }


    public AasRegistryContext asContext() {
        return new AasRegistryContext.Builder()
                .defaultAccessPolicyDefinitionId(defaultAccessPolicyDefinitionId())
                .defaultContractPolicyDefinitionId(defaultContractPolicyDefinitionId())
                .uri(this.url())
                .authenticationMethod(this.auth())
                .allowSelfSigned(Configuration.getInstance().isAllowSelfSignedCertificates())
                .build();
    }
}
