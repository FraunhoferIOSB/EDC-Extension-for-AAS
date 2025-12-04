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
package de.fraunhofer.iosb.client.policy;

import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;


class PolicyControllerTest {

    private static PolicyDefinition mockPolicyDefinition;
    private PolicyController testSubject;


    @BeforeAll
    public static void initialize() {
        var mockAsset = Asset.Builder.newInstance().id("test-asset").build();

        var mockPolicy = Policy.Builder.newInstance().target(mockAsset.getId()).build();

        mockPolicyDefinition = PolicyDefinition.Builder.newInstance()
                .policy(mockPolicy).build();
    }


    @BeforeEach
    void setUp() {
        testSubject = new PolicyController(new ConsoleMonitor(),
                mock(CatalogService.class),
                mock(ParticipantContext.class),
                mock(TypeTransformerRegistry.class),
                mock(Config.class));
    }


    @Test
    public void addAcceptedContractOffersTest() {
        var mockPolicyDefinitionsAsList = new ArrayList<PolicyDefinition>();
        mockPolicyDefinitionsAsList.add(mockPolicyDefinition); // ClientEndpoint creates ArrayList
        var offers = new PolicyDefinition[] { mockPolicyDefinition };

        testSubject.addAcceptedPolicyDefinitions(offers);

        assertEquals(mockPolicyDefinitionsAsList, testSubject.getAcceptedPolicyDefinitions().getEntity());
    }


    @Test
    public void updateAcceptedContractOfferTest() {
        var offers = new PolicyDefinition[] { mockPolicyDefinition };

        testSubject.addAcceptedPolicyDefinitions(offers);

        var mockPolicy = Policy.Builder.newInstance().build();
        var mockUpdatedContractOffer = PolicyDefinition.Builder.newInstance()
                .id(mockPolicyDefinition.getId()) // Same id <-> same offer
                .policy(mockPolicy)
                .build();

        var mockContractOfferAsList = new ArrayList<PolicyDefinition>();
        mockContractOfferAsList.add(mockUpdatedContractOffer); // ClientEndpoint creates ArrayList
        testSubject.updateAcceptedPolicyDefinition(mockUpdatedContractOffer);

        assertEquals(mockContractOfferAsList, testSubject.getAcceptedPolicyDefinitions().getEntity());

    }


    @Test
    public void getAcceptedContractOffersTest() {
        assertEquals(Response.Status.OK.getStatusCode(), testSubject.getAcceptedPolicyDefinitions().getStatus());
    }
}
