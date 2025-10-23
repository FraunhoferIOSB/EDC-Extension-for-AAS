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
package de.fraunhofer.iosb.aas.lib.spi;

import de.fraunhofer.iosb.aas.lib.model.impl.Service;
import de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress;
import org.eclipse.digitaltwin.aas4j.v3.model.Key;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultKey;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

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
        String path = "/submodel-elements/";
        for (int i = 0; i < 100; i++) {
            keys.add(getKey(KeyTypes.SUBMODEL_ELEMENT_COLLECTION, "smc%s".formatted(i)));
            path = path.concat("smc%s.".formatted(i));
        }

        keys.add(getKey(KeyTypes.SUBMODEL_ELEMENT, "sme"));
        path = path.concat("sme");

        var referenceChain = new DefaultReference.Builder()
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