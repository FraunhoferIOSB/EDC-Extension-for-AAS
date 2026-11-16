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

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import de.fraunhofer.iosb.ilt.dataspace.client.testutils.CatalogFixtures;
import de.fraunhofer.iosb.ilt.dataspace.client.testutils.DspClientTestBase;
import de.fraunhofer.iosb.ilt.dataspace.client.testutils.FakeContractNegotiationService;
import okhttp3.Response;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Layer-3 policy-gating test (plan case 3.2) over the full catalog flow: like {@code DspClientFullFlowHttpTest} the
 * runtime keeps the real control-plane catalog machinery (aggregate-services CatalogServiceImpl, DSP-2025 request
 * dispatch against WireMock, real JsonLd expansion and dsp-catalog-transform-2025 chain), but with the production
 * default {@code edc.client.dsp.policy.accepted.all=false} and nothing POSTed to the accepted-policy API.
 *
 * <p>Even though the real catalog request reaches the provider and the offer is expanded successfully, the acceptable
 * policy check rejects the offer (Layer-1 finding: POSTed policies can never match because the policy-model rule
 * classes lack {@code equals}), so the controller answers 400 and the (faked) negotiation service is never called.
 */
class DspClientFullFlowPolicyGatingHttpTest extends DspClientTestBase {

    private static final int GATING_PORT = 8195;
    private static final String ASSET_ID = "-1379397334";
    private static final String COUNTER_PARTY_ID = "provider";
    private static final String DSP_VERSION_PATH = "/2025/1";
    private static final String CATALOG_REQUEST_PATH = DSP_VERSION_PATH + "/catalog/request";

    private static final FakeContractNegotiationService NEGOTIATION_SERVICE = new FakeContractNegotiationService();

    @RegisterExtension
    static WireMockExtension provider = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @RegisterExtension
    static RuntimePerClassExtension runtime = fullFlowRuntime(GATING_PORT, "DspClient Full Flow Policy Gating");

    @BeforeAll
    static void awaitJetty() {
        awaitRuntimeReadiness(negotiateUrl(GATING_PORT));
    }


    @AfterAll
    static void tearDownProvider() {
        provider.shutdownServer();
    }


    @BeforeEach
    void arrange() {
        NEGOTIATION_SERVICE.reset();
        stubCatalogResponse(CatalogFixtures.catalogBytes());
    }


    @AfterEach
    void clean() {
        provider.resetAll();
    }


    @Override
    protected int runtimePort() {
        return GATING_PORT;
    }


    /**
     * Case 3.2: with the default {@code acceptAllPolicies=false} and no accepted policy definition stored, the offer
     * of the really fetched and really expanded catalog is not acceptable; the PolicyService answers notFound and the
     * controller maps it to 400 InvalidRequest. The WireMock request proves the full supply chain (real dispatch,
     * provider catalog, real expansion and transformation) ran BEFORE the gating decision.
     */
    @Test
    void policyGatingRejectsOfferOfReallyFetchedCatalogTest() throws Exception {
        try (Response response = postNegotiate(dspClientPullRequest(), false)) {
            assertEquals(400, response.code(), "without an acceptable policy the offer is rejected");
            assertEquals("application/json", contentType(response));
            var errors = MAPPER.readTree(body(response));
            assertTrue(errors.isArray());
            assertEquals("InvalidRequest", errors.get(0).path("type").asText());
            var message = errors.get(0).path("message").asText();
            assertEquals("Could not find acceptable policyDefinition", message);
        }

        assertEquals(0, NEGOTIATION_SERVICE.callCount(), "no negotiation is initiated without an acceptable policy");
        provider.verify(1, postRequestedFor(urlPathEqualTo(CATALOG_REQUEST_PATH)));
    }


    /**
     * Full-flow runtime with the production-default policy gating kept (acceptAllPolicies=false); retry and
     * participant-id settings mirror {@code DspClientFullFlowHttpTest} (see the notes there).
     */
    private static RuntimePerClassExtension fullFlowRuntime(int port, String name) {
        var config = new HashMap<String, String>();
        config.put("edc.participant.id", PARTICIPANT_CONTEXT_ID);
        config.put("edc.core.retry.retries.max", "2");
        config.put("edc.core.retry.backoff.min", "5");
        config.put("edc.core.retry.backoff.max", "25");
        var extension = runtime(name, port, Map.copyOf(config));
        extension.registerServiceMock(ContractNegotiationService.class, NEGOTIATION_SERVICE);
        return extension;
    }


    private String counterPartyAddress() {
        return provider.baseUrl() + DSP_VERSION_PATH;
    }


    private String dspClientPullRequest() {
        return """
                {"counterPartyAddress": "%s", "counterPartyId": "%s", "assetId": "%s", "participantContextId": "%s"}\
                """.formatted(counterPartyAddress(), COUNTER_PARTY_ID, ASSET_ID, PARTICIPANT_CONTEXT_ID);
    }


    private void stubCatalogResponse(byte[] catalogBytes) {
        provider.stubFor(post(urlPathEqualTo(CATALOG_REQUEST_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(catalogBytes)));
    }
}
