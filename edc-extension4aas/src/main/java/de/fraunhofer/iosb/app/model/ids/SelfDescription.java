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
package de.fraunhofer.iosb.app.model.ids;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.app.model.aas.CustomAssetAdministrationShellEnvironment;
import org.eclipse.edc.spi.EdcException;

import java.util.Objects;

/**
 * Self-description holding the structure of an AAS model
 */
@JsonInclude(Include.NON_NULL)
public class SelfDescription {

    private final CustomAssetAdministrationShellEnvironment aasEnv; // enriched with contractIds
    private final ObjectMapper objectMapper;

    public SelfDescription(CustomAssetAdministrationShellEnvironment environment) {
        Objects.requireNonNull(environment);

        this.aasEnv = environment;
        this.objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(Include.NON_NULL);
    }

    /**
     * Serializes the underlying CustomAASEnvironment using the
     * com.fasterxml.jackson.databind.ObjectMapper.writeValueAsString method.
     */
    @Override
    public String toString() {
        try {
            return objectMapper.writeValueAsString(aasEnv);
        } catch (JsonProcessingException e) {
            throw new EdcException("Could not serialize self description", e);
        }
    }

    /**
     * Converts this SelfDescription to a JsonNode
     *
     * @return JsonNode representation of this SelfDescription.
     */
    public JsonNode toJsonNode() {
        try {
            return objectMapper.readTree(this.toString());
        } catch (JsonProcessingException e) {
            throw new EdcException("Could not convert SelfDescription to JsonNode", e);
        }
    }

    /**
     * Returns the underlying environment.
     *
     * @return The underlying environment
     */
    public CustomAssetAdministrationShellEnvironment getEnvironment() {
        return aasEnv;
    }
}
