package de.fraunhofer.iosb.dataplane.aas.spi;


import org.eclipse.digitaltwin.aas4j.v3.model.Key;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.ReferenceTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultKey;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AasDataAddressTest {

    @Test
    void test_build_accessUrlBuiltCorrectly() {
        var addressBuilder = AasDataAddress.Builder.newInstance();

        addressBuilder.baseUrl("http://localhost:8080");
        addressBuilder.path("/path/to/resource");

        var address = addressBuilder.build();

        assertEquals("http://localhost:8080", address.getBaseUrl());

        assertEquals("/path/to/resource", address.getPath());
    }

    @Test
    void test_build_accessUrlBuiltCorrectlyWithProvider() {
        var addressBuilder = AasDataAddress.Builder.newInstance();

        addressBuilder.baseUrl("http://aas-provider:8081");

        addressBuilder.path("/path/to/resource");

        var address = addressBuilder.build();
        assertEquals("http://aas-provider:8081", address.getBaseUrl());

        assertEquals("/path/to/resource", address.getPath());
    }

    @Test
    void test_build_returnCorrectReferenceChainAsPathNested() {
        String smIdShort = "sm";

        var keys = new ArrayList<Key>();
        String path = "submodels/";

        String smId = UUID.randomUUID().toString();
        keys.add(getKey(KeyTypes.SUBMODEL, smId));

        path = path.concat(Base64.getEncoder().encodeToString(smId.getBytes(StandardCharsets.UTF_8)));


        path = path.concat("/submodel-elements/");
        for (int i = 0; i < 100; i++) {
            keys.add(getKey(KeyTypes.SUBMODEL_ELEMENT_COLLECTION, "smc%s".formatted(i)));
            path = path.concat("smc%s.".formatted(i));
        }

        keys.add(getKey(KeyTypes.SUBMODEL_ELEMENT, "sme"));
        path = path.concat("sme");

        var referenceChain = new DefaultReference.Builder()
                .type(ReferenceTypes.MODEL_REFERENCE)
                .keys(keys)
                .build();

        assertEquals(path.formatted(Encoder.encodeBase64(smIdShort)),
                AasDataAddress.Builder.newInstance().referenceChain(referenceChain).build().getPath());
    }

    @Test
    void test_build_returnCorrectReferenceChainAsPathSubmodel() {
        String smIdShort = "sm";
        var referenceChain = new DefaultReference.Builder()
                .keys(List.of(
                        getKey(KeyTypes.SUBMODEL, smIdShort)
                ))
                .build();

        assertEquals("submodels/%s".formatted(Encoder.encodeBase64(smIdShort)),
                AasDataAddress.Builder.newInstance().referenceChain(referenceChain).build().getPath());
    }

    @Test
    void test_build_returnCorrectReferenceChainAsPathShell() {
        String shellIdShort = "shell";
        var referenceChain = new DefaultReference.Builder()
                .keys(List.of(getKey(KeyTypes.ASSET_ADMINISTRATION_SHELL, shellIdShort)))
                .build();

        assertEquals("shells/%s".formatted(Encoder.encodeBase64(shellIdShort)),
                AasDataAddress.Builder.newInstance().referenceChain(referenceChain).build().getPath());
    }

    @Test
    void test_build_returnCorrectReferenceChainAsPathConceptDescription() {
        String cdIdShort = "cd";
        var referenceChain = new DefaultReference.Builder()
                .keys(List.of(getKey(KeyTypes.CONCEPT_DESCRIPTION, cdIdShort)))
                .build();

        assertEquals("concept-descriptions/%s".formatted(Encoder.encodeBase64(cdIdShort)),
                AasDataAddress.Builder.newInstance().referenceChain(referenceChain).build().getPath());
    }

    private Key getKey(KeyTypes keyType, String idShort) {
        return new DefaultKey.Builder()
                .type(keyType)
                .value(idShort)
                .build();
    }

    private static class Encoder {
        private static final java.util.Base64.Encoder ENC = Base64.getEncoder();

        static String encodeBase64(String toEncode) {
            return ENC.encodeToString(toEncode.getBytes(StandardCharsets.UTF_8));
        }
    }
}