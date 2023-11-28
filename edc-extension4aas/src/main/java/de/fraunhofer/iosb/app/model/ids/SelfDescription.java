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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.app.Logger;
import de.fraunhofer.iosb.app.model.aas.CustomAssetAdministrationShellEnvironment;

import java.util.Objects;

/**
 * Self description holding the structure of an AAS model
 */
public class SelfDescription {

    private static final Logger LOGGER = Logger.getInstance();

    private final CustomAssetAdministrationShellEnvironment aasEnv; // enriched with contractIds
    private final ObjectMapper objectMapper;

    public SelfDescription(CustomAssetAdministrationShellEnvironment newSelfDescription) {
        Objects.requireNonNull(newSelfDescription);
        this.aasEnv = newSelfDescription;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Serializes the underlying CustomAASEnvironment using the
     * com.fasterxml.jackson.databind.ObjectMapper.writeValueAsString method.
     */
    @Override
    public String toString() {
        String aasEnvString;
        try {
            aasEnvString = objectMapper.writeValueAsString(aasEnv);
        } catch (JsonProcessingException e) {
            LOGGER.severe("Could not serialize self description", e);
            return null;
        }
        return aasEnvString;
    }

    public JsonNode toJsonNode() {
        JsonNode aasEnvString;
        try {
            aasEnvString = objectMapper.readTree(this.toString());
        } catch (JsonProcessingException e) {
            LOGGER.severe("Could not serialize self description", e);
            return null;
        }
        return aasEnvString;
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
