package de.fraunhofer.iosb.aas.lib.model.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.ReferenceTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultKey;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static de.fraunhofer.iosb.aas.test.FileManager.loadResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceTest {

    private final Reference referenceToMatch = new DefaultReference.Builder()
            .type(ReferenceTypes.MODEL_REFERENCE)
            .keys(List.of(new DefaultKey.Builder()
                            .type(KeyTypes.SUBMODEL)
                            .value("xyz")
                            .build(),
                    new DefaultKey.Builder()
                            .type(KeyTypes.SUBMODEL_ELEMENT_COLLECTION)
                            .value("coll")
                            .build(),
                    new DefaultKey.Builder()
                            .type(KeyTypes.PROPERTY)
                            .value("prop")
                            .build()
            ))
            .build();


    @Test
    void createService_withSelectiveElements() throws JsonProcessingException {
        String serviceJsonString = loadResource("service.json");

        var service = new ObjectMapper().readValue(serviceJsonString, Service.class);

        assertEquals("https://localhost:443/api/v3.0", service.getAccessUrl().toString());
        assertEquals(Map.of("x-api-key", "password"), service.getHeaders());

        assertEquals(referenceToMatch, service.getPolicyBindings().get(0).referredElement());

        assertEquals("default-access-policy",
                service.getPolicyBindings().get(0).accessPolicyDefinitionId());
        assertEquals("default-usage-policy",
                service.getPolicyBindings().get(0).contractPolicyDefinitionId());
    }

    @Test
    void createService_noSelectiveElements() throws JsonProcessingException {
        String serviceJsonString = loadResource("service_no_selection.json");

        var service = new ObjectMapper().readValue(serviceJsonString, Service.class);

        assertEquals("https://localhost:443/api/v3.0", service.getAccessUrl().toString());
        assertEquals(Map.of("x-api-key", "password"), service.getHeaders());

        assertTrue(service.getPolicyBindings().isEmpty());

    }

    @Test
    void createService_noAuth() throws JsonProcessingException {
        String serviceJsonString = loadResource("service_no_auth.json");

        var service = new ObjectMapper().readValue(serviceJsonString, Service.class);

        assertEquals("https://localhost:443/api/v3.0", service.getAccessUrl().toString());

        assertEquals(Map.of(), service.getHeaders());


        assertEquals(referenceToMatch, service.getPolicyBindings().get(0).referredElement());

        assertEquals("default-access-policy",
                service.getPolicyBindings().get(0).accessPolicyDefinitionId());
        assertEquals("default-usage-policy",
                service.getPolicyBindings().get(0).contractPolicyDefinitionId());

    }
}