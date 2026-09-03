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
package de.fraunhofer.iosb.ilt.dataspace.aas.lib.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.ilt.dataspace.aas.test.FileManager;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class PolicyBindingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();


    @Test
    void deserialize_shouldUseJsonCreatorAndParseReference() throws Exception {
        String json = FileManager.loadResource("policyBinding.json");

        PolicyBinding binding = objectMapper.readValue(json, PolicyBinding.class);

        assertNotNull(binding);
        Reference reference = binding.referredElement();
        assertNotNull(reference);
        assertEquals(KeyTypes.SUBMODEL, reference.getKeys().get(0).getType());
        assertEquals("xyz", reference.getKeys().get(0).getValue());
        assertEquals("default-access-policy", binding.accessPolicyDefinitionId());
        assertEquals("default-usage-policy", binding.contractPolicyDefinitionId());
        Map<String, Object> dataAddressProperties = binding.dataAddressProperties();
        assertNotNull(dataAddressProperties);
        assertEquals(2, dataAddressProperties.size());
        assertEquals("POST", dataAddressProperties.get("method"));
        assertEquals("value", dataAddressProperties.get("custom"));
    }


    @Test
    void deserialize_withoutDataAddressProperties_shouldYieldEmptyMap() throws Exception {
        String json = """
                {
                  "referredElement": "[ModelRef](Submodel)xyz",
                  "accessPolicyId": "access",
                  "usagePolicyId": "usage"
                }""";

        PolicyBinding binding = objectMapper.readValue(json, PolicyBinding.class);

        assertNotNull(binding);
        assertTrue(binding.dataAddressProperties().isEmpty());
    }
}
