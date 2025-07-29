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
package de.fraunhofer.iosb.ilt.api.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class EndpointTest {

    Endpoint endpoint;

    public static Endpoint createNormalEndpoint() {
        return new Endpoint("/api/suffix", HttpMethod.HEAD,
                Map.of("api-key-super-secret", List.of("12345678")));
    }

    @Test
    void isCoveredByMoreValuesInList() {
        endpoint = createNormalEndpoint();

        var coveringEndpoint = new Endpoint("/api/suffix", HttpMethod.HEAD,
                Map.of("api-key-super-secret", List.of("12345678", "another-passkey-wow")));
        assertTrue(endpoint.isCoveredBy(coveringEndpoint));
    }

    @Test
    void isCoveredByMoreValuesInMap() {
        endpoint = createNormalEndpoint();

        var coveringEndpoint = new Endpoint("/api/suffix", HttpMethod.HEAD,
                Map.of("api-key-super-secret", List.of("12345678", "another-passkey-wow"),
                        "nother-key", List.of("Wowie!")));

        assertTrue(endpoint.isCoveredBy(coveringEndpoint));
    }

    @Test
    void isCoveredByEqualEndpoint() {
        endpoint = createNormalEndpoint();

        var coveringEndpoint = createNormalEndpoint();

        assertTrue(endpoint.isCoveredBy(coveringEndpoint));
    }

    @Test
    void isCoveredByFailOtherValue() {
        endpoint = createNormalEndpoint();

        var coveringEndpoint = new Endpoint("/api/suffix", HttpMethod.HEAD,
                Map.of("api-key-super-secret", List.of("87654321")));

        assertFalse(endpoint.isCoveredBy(coveringEndpoint));
    }

    @Test
    void isCoveredByFailOtherKey() {
        endpoint = createNormalEndpoint();

        var coveringEndpoint = new Endpoint("/api/suffix", HttpMethod.HEAD,
                Map.of("api-key-super-secret2", List.of("12345678")));

        assertFalse(endpoint.isCoveredBy(coveringEndpoint));
    }

    @Test
    void createWithNullValues() {
        endpoint = createNormalEndpoint();

        try {
            new Endpoint(null, HttpMethod.HEAD, Map.of("", List.of("")));
            fail();
        } catch (NullPointerException expected) {
        }

    }

}