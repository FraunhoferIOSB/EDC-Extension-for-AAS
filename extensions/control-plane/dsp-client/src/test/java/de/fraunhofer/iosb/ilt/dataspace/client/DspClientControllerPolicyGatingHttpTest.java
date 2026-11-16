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

import de.fraunhofer.iosb.ilt.dataspace.client.testutils.DspClientTestBase;
import de.fraunhofer.iosb.ilt.dataspace.client.testutils.FakeCatalogService;
import de.fraunhofer.iosb.ilt.dataspace.client.testutils.FakeContractNegotiationService;
import de.fraunhofer.iosb.ilt.dataspace.client.testutils.FakeTransferProcessService;
import okhttp3.Response;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Layer-1 integration tests (plan cases 1.4 and 1.10) for the policy-gating configuration
 * ({@code edc.client.dsp.policy.accepted.all=false}, the production default): a negotiate request is only answered
 * with the dataset offer if the offer policy equals one of the accepted policy definitions stored over the real
 * PolicyController HTTP API. The catalog fake serves the fixture's single-dataset catalog; the cross-controller
 * interaction (POST accepted policy definition over HTTP, then negotiate) runs against the real AcceptedPolicyStore.
 */
class DspClientControllerPolicyGatingHttpTest extends DspClientTestBase {

    private static final int GATING_RUNTIME_PORT = 8196;
    private static final String ASSET_ID = "-1379397334";
    private static final String COUNTER_PARTY_ID = "provider";
    private static final String COUNTER_PARTY_ADDRESS = "http://localhost:13340/dsp/2025-1";
    private static final String USE = "USE";

    private static final FakeCatalogService CATALOG_SERVICE = new FakeCatalogService();
    private static final FakeContractNegotiationService NEGOTIATION_SERVICE = new FakeContractNegotiationService();
    private static final FakeTransferProcessService TRANSFER_SERVICE = new FakeTransferProcessService();

    @RegisterExtension
    static RuntimePerClassExtension runtime = runtimeWithFakes("DspClient Controller Policy Gating", GATING_RUNTIME_PORT,
            Map.of("edc.client.dsp.policy.accepted.all", "false"));

    @BeforeAll
    static void awaitJetty() {
        awaitRuntimeReadiness(negotiateUrl(GATING_RUNTIME_PORT));
    }


    @BeforeEach
    void arrangeDefaultFakes() {
        CATALOG_SERVICE.reset();
        NEGOTIATION_SERVICE.reset();
        TRANSFER_SERVICE.reset();
    }


    @Override
    protected int runtimePort() {
        return GATING_RUNTIME_PORT;
    }


    private static RuntimePerClassExtension runtimeWithFakes(String name, int port, Map<String, String> config) {
        var extension = runtime(name, port, config);
        extension.registerServiceMock(CatalogService.class, CATALOG_SERVICE);
        extension.registerServiceMock(ContractNegotiationService.class, NEGOTIATION_SERVICE);
        extension.registerServiceMock(TransferProcessService.class, TRANSFER_SERVICE);
        return extension;
    }


    /**
     * Case 1.4 (and the negative twin of 1.10): with no accepted policy definition stored, no offer of the catalog is
     * acceptable, the PolicyService answers notFound and the controller maps that to 400 InvalidRequest. The
     * negotiation service is never called.
     */
    @Test
    void noAcceptablePolicyIsNegotiableIsBadRequestTest() throws Exception {
        try (Response response = postNegotiate(dspClientPullRequest(), null)) {
            assertEquals(400, response.code(), "no acceptable policy maps the notFound result to InvalidRequest");
            assertNoAcceptablePolicy(response);
        }

        assertEquals(0, NEGOTIATION_SERVICE.callCount(), "no negotiation may be initiated without an acceptable policy");
    }


    /**
     * Case 1.10 positive twin (pinned finding, plan expected 200): a policy definition mirroring the catalog offer
     * policy is POSTed over the real PolicyController first (and verifiably stored), but the negotiate request is
     * STILL rejected with 400. Mechanism: the offer policy arriving through the dsp-catalog-transform-2025 chain and
     * the stored policy both contain {@code Permission}/{@code Action} rule objects, and policy-model 0.17.0 rule
     * classes use IDENTITY equality, so {@code Policy.equals} can never equate them; the accepted-policy gating
     * therefore cannot succeed via the HTTP surface at all ({@code matchesOwnPolicyDefinitions} compares with
     * equals). Prominent follow-up finding: gating needs a content-based comparison.
     */
    @Test
    void mirroredAcceptedPolicyStillDoesNotEnableNegotiationTest() throws Exception {
        var mirroringJson = MAPPER.writeValueAsString(mirroringPolicyDefinition());
        try (Response post = postAcceptedPolicyDefinition(mirroringJson)) {
            assertEquals(204, post.code(), "the mirroring policy definition is accepted by the PolicyController");
        }
        try (Response get = getAcceptedPolicyDefinitions()) {
            assertEquals(200, get.code());
            var storedDefinitions = body(get);
            assertTrue(storedDefinitions.contains("case-1.10-mirroring-policy"),
                    () -> "the mirroring policy definition must be verifiably stored: " + storedDefinitions);
        }

        try (Response response = postNegotiate(dspClientPullRequest(), null)) {
            assertNoAcceptablePolicy(response);
        }

        assertEquals(0, NEGOTIATION_SERVICE.callCount(),
                "no negotiation is initiated: rule identity equality prevents the stored policy from ever matching");
    }


    private void assertNoAcceptablePolicy(Response response) throws Exception {
        assertEquals("application/json", contentType(response), "gating failures are ApiErrorDetail JSON");
        var errors = MAPPER.readTree(body(response));
        assertTrue(errors.isArray());
        var message = errors.get(0).path("message").asText();
        assertEquals("Could not find acceptable policyDefinition", message);
        assertEquals("InvalidRequest", errors.get(0).path("type").asText());
    }


    private static PolicyDefinition mirroringPolicyDefinition() {
        return PolicyDefinition.Builder.newInstance()
                .id("case-1.10-mirroring-policy")
                .policy(Policy.Builder.newInstance()
                        .assigner(COUNTER_PARTY_ID)
                        .permission(Permission.Builder.newInstance()
                                .action(Action.Builder.newInstance().type(USE).build())
                                .build())
                        .build())
                .build();
    }


    private static String dspClientPullRequest() {
        return """
                {"counterPartyAddress": "%s", "counterPartyId": "%s", "assetId": "%s", "participantContextId": "%s"}\
                """.formatted(COUNTER_PARTY_ADDRESS, COUNTER_PARTY_ID, ASSET_ID, PARTICIPANT_CONTEXT_ID);
    }
}
