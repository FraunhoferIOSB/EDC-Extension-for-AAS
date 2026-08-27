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
package de.fraunhofer.iosb.aas.lib.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;

import java.util.Objects;

import static de.fraunhofer.iosb.constants.AasConstants.DEFAULT_ACCESS_POLICY_DEFINITION_ID;
import static de.fraunhofer.iosb.constants.AasConstants.DEFAULT_USAGE_POLICY_DEFINITION_ID;


/**
 * Binds an AAS element to access and usage policy. If any of the policies are null, a default policy is to be used.
 */
public record PolicyBinding(Reference referredElement, @JsonProperty("accessPolicyId") String accessPolicyDefinitionId,
        @JsonProperty("usagePolicyId") String contractPolicyDefinitionId) {

    public PolicyBinding {
        Objects.requireNonNull(referredElement);
    }


    public static PolicyBinding ofDefaults(Reference reference) {
        return new PolicyBinding(reference, DEFAULT_ACCESS_POLICY_DEFINITION_ID, DEFAULT_USAGE_POLICY_DEFINITION_ID);
    }

}
