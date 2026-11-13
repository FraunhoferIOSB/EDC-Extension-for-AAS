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
package de.fraunhofer.iosb.ilt.dataspace.client;

import de.fraunhofer.iosb.ilt.dataspace.client.dto.PullRequestDTO;
import de.fraunhofer.iosb.ilt.dataspace.client.policy.PolicyService;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.UUID;

import static java.lang.String.format;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;


public class ClientEndpointTest {

    private static Monitor monitor;

    private static String uri;
    private DspClientController clientEndpoint;

    @BeforeAll
    public static void initialize() {
        monitor = mock();
        int port = getFreePort();
        uri = URI.create(format("http://localhost:%s", port)).toString();
    }


    @BeforeEach
    public void setup() {
        var policyService = new PolicyService(mock(), mock(), mock(), mock(), mock(), mock(), mock());
        clientEndpoint = new DspClientController(monitor, policyService, mock(), mock());
    }


    @Test
    public void negotiateContractTest() {
        assertThrows(EdcException.class, () -> clientEndpoint.negotiateContract(true, new PullRequestDTO(uri, UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                "context")));
    }
}
