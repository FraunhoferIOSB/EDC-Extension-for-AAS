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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.fraunhofer.iosb.ilt.faaast.service.util.ReferenceHelper;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Binds an AAS element to access and usage policy.
 * If any of the policies are null, a default policy is to be used.
 */
@JsonDeserialize(builder = PolicyBinding.Builder.class)
public record PolicyBinding(Reference referredElement, String accessPolicyDefinitionId, String contractPolicyDefinitionId) {
    public PolicyBinding(Reference referredElement, String accessPolicyDefinitionId, String contractPolicyDefinitionId) {
        this.referredElement = Objects.requireNonNull(referredElement);
        this.accessPolicyDefinitionId = accessPolicyDefinitionId;
        this.contractPolicyDefinitionId = contractPolicyDefinitionId;
    }

    @Override
    public @NotNull Reference referredElement() {
        return referredElement;
    }

    @Override
    public @Nullable String accessPolicyDefinitionId() {
        return accessPolicyDefinitionId;
    }

    @Override
    public @Nullable String contractPolicyDefinitionId() {
        return contractPolicyDefinitionId;
    }

    public static class Builder {
        private Reference referredElement;
        private String accessPolicyDefinitionId;
        private String contractPolicyDefinitionId;

        public Builder withReferredElement(String referredElement) {
            this.referredElement = Objects.requireNonNull(ReferenceHelper.parse(referredElement));
            return this;
        }

        @JsonProperty("accessPolicyId")
        public Builder withAccessPolicyDefinitionId(String accessPolicyDefinitionId) {
            this.accessPolicyDefinitionId = accessPolicyDefinitionId;
            return this;
        }

        @JsonProperty("usagePolicyId")
        public Builder withContractPolicyDefinitionId(String contractPolicyDefinitionId) {
            this.contractPolicyDefinitionId = contractPolicyDefinitionId;
            return this;
        }

        public PolicyBinding build() {
            return new PolicyBinding(referredElement, accessPolicyDefinitionId, contractPolicyDefinitionId);
        }
    }
}
