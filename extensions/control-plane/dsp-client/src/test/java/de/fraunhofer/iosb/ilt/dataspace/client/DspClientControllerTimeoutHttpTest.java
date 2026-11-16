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
import de.fraunhofer.iosb.ilt.dataspace.client.testutils.NegotiationFlow;
import okhttp3.Response;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Layer-1 integration tests (plan case 1.7 and the transfer-leg timeout twin) on a runtime configured with
 * {@code edc.client.dsp.timeout.negotiation=1} and {@code edc.client.dsp.timeout.transfer=1}, so the production
 * future-waits time out after a second instead of the default 20.
 *
 * <p>All negotiation failures (timeout and termination) are surfaced by the controller as 400 InvalidRequest with the
 * failure detail; the plan's expectation that the 1.7 message contains the provider and asset is aspirational — the
 * production messages do not carry them (documented deviation).
 */
class DspClientControllerTimeoutHttpTest extends DspClientTestBase {

    private static final int TIMEOUT_RUNTIME_PORT = 8197;
    private static final String ASSET_ID = "-1379397334";
    private static final String COUNTER_PARTY_ID = "provider";
    private static final String COUNTER_PARTY_ADDRESS = "http://localhost:13340/dsp/2025-1";

    private static final FakeCatalogService CATALOG_SERVICE = new FakeCatalogService();
    private static final FakeContractNegotiationService NEGOTIATION_SERVICE = new FakeContractNegotiationService();
    private static final FakeTransferProcessService TRANSFER_SERVICE = new FakeTransferProcessService();

    @RegisterExtension
    static RuntimePerClassExtension runtime = runtimeWithFakes("DspClient Controller Timeouts", TIMEOUT_RUNTIME_PORT,
            Map.of(
                    "edc.client.dsp.policy.accepted.all", "true",
                    "edc.client.dsp.timeout.negotiation", "1",
                    "edc.client.dsp.timeout.transfer", "1"));

    @BeforeAll
    static void awaitJetty() {
        awaitRuntimeReadiness(negotiateUrl(TIMEOUT_RUNTIME_PORT));
    }


    @BeforeEach
    void arrangeDefaultFakes() {
        CATALOG_SERVICE.reset();
        NEGOTIATION_SERVICE.reset();
        TRANSFER_SERVICE.reset();
        TRANSFER_SERVICE.respondWithEdr(ASSET_ID, NegotiationFlow.edrDataAddress("https://provider.example/proxy/edr", "unused"));
    }


    @AfterEach
    void cleanNegotiationStore() {
        NegotiationFlow.cleanupNegotiationStore(runtime);
    }


    @Override
    protected int runtimePort() {
        return TIMEOUT_RUNTIME_PORT;
    }


    private static RuntimePerClassExtension runtimeWithFakes(String name, int port, Map<String, String> config) {
        var extension = runtime(name, port, config);
        extension.registerServiceMock(CatalogService.class, CATALOG_SERVICE);
        extension.registerServiceMock(ContractNegotiationService.class, NEGOTIATION_SERVICE);
        extension.registerServiceMock(TransferProcessService.class, TRANSFER_SERVICE);
        return extension;
    }


    /**
     * Case 1.7 (timeout variant): the negotiation never reaches a terminal state, so the Negotiator hits the
     * configured 1-second timeout and the controller answers 400 with the timeout detail. The message does not contain
     * the provider and asset the plan expected (documented deviation).
     */
    @Test
    void negotiationTimesOutIsBadRequestTest() throws Exception {
        var responseFuture = CompletableFuture.supplyAsync(() -> postNegotiate(dspClientPullRequest(), false));

        NegotiationFlow.awaitValue(NEGOTIATION_SERVICE::capturedRequest, "contract request");
        try (Response response = responseFuture.join()) {
            assertEquals(400, response.code(), "negotiation timeout surfaces as InvalidRequest");
            assertEquals("application/json", contentType(response));
            var errors = MAPPER.readTree(body(response));
            assertTrue(errors.isArray());
            var message = errors.get(0).path("message").asText();
            assertTrue(message.contains("Timed out while waiting for agreement"), () -> "unexpected body: " + message);
        }

        assertEquals(1, NEGOTIATION_SERVICE.callCount(), "the negotiation was initiated before the wait timed out");
    }


    /**
     * Case 1.7 (terminated variant): the provider terminates the negotiation; the Listener future completes
     * exceptionally and the controller answers 400 with the termination detail.
     */
    @Test
    void terminatedNegotiationIsBadRequestTest() throws Exception {
        var responseFuture = CompletableFuture.supplyAsync(() -> postNegotiate(dspClientPullRequest(), false));

        NegotiationFlow.awaitValue(NEGOTIATION_SERVICE::capturedRequest, "contract request");
        var terminated = NegotiationFlow.terminatedNegotiation(NEGOTIATION_SERVICE.lastNegotiation(), "offer declined by provider");
        NegotiationFlow.fireUntil(responseFuture::isDone, "terminated negotiation",
                () -> NegotiationFlow.fireNegotiationTerminated(runtime, terminated));

        try (Response response = responseFuture.join()) {
            assertEquals(400, response.code(), "a terminated negotiation surfaces as InvalidRequest");
            assertEquals("application/json", contentType(response));
            var errors = MAPPER.readTree(body(response));
            assertTrue(errors.isArray());
            var message = errors.get(0).path("message").asText();
            assertTrue(message.contains("ExecutionException thrown while waiting for agreement"), () -> "unexpected body: " + message);
            assertTrue(message.contains("Negotiation with ID %s was terminated. Reason: offer declined by provider"
                    .formatted(NEGOTIATION_SERVICE.lastNegotiationId())), () -> "unexpected body: " + message);
        }
    }


    /**
     * Pinned transfer-leg twin of 1.7: the agreement completes but the transfer never starts, so the
     * PullTransferInitiator hits the 1-second transfer timeout. That path produces a {@code StatusResult} carrying NO
     * failure messages, and the controller's {@code getFailureMessages().getLast()} then throws
     * NoSuchElementException, which the fallback mapper answers with an entity-less bare 500 (the embedded Jetty
     * fills its HTML error page into the empty entity) — an error-handling gap next to the 422 the terminated
     * transfer produces.
     */
    @Test
    void transferTimesOutWithBareServerErrorTest() throws Exception {
        var agreementId = "agreement-" + UUID.randomUUID();
        var responseFuture = CompletableFuture.supplyAsync(() -> postNegotiate(dspClientPullRequest(), null));

        NegotiationFlow.awaitValue(NEGOTIATION_SERVICE::capturedRequest, "contract request");
        var finalized = completeNegotiation(agreementId);
        NegotiationFlow.fireUntil(() -> TRANSFER_SERVICE.capturedRequest() != null, "negotiation into transfer",
                () -> NegotiationFlow.fireNegotiationFinalized(runtime, finalized));

        try (Response response = responseFuture.join()) {
            assertEquals(500, response.code(),
                    "(ERROR_RETRY carries no messages, so the controller crashes reading the failure detail: bare 500)");
            var failureDetail = body(response);
            assertTrue(failureDetail.startsWith("<html>"),
                    () -> "entity-less 500 gets the container's default HTML error page: " + failureDetail);
        }
    }


    private ContractNegotiation completeNegotiation(String agreementId) {
        var capturedRequest = NEGOTIATION_SERVICE.capturedRequest();
        var offerPolicy = capturedRequest.getContractOffer().getPolicy();
        var agreement = NegotiationFlow.agreement(agreementId, offerPolicy.getAssigner(), "dsp-client-test",
                capturedRequest.getContractOffer().getAssetId(), offerPolicy);
        var finalized = NegotiationFlow.finalizedNegotiation(NEGOTIATION_SERVICE.lastNegotiation(), agreement);
        NegotiationFlow.saveNegotiation(runtime, finalized);
        return finalized;
    }


    private static String dspClientPullRequest() {
        return """
                {"counterPartyAddress": "%s", "counterPartyId": "%s", "assetId": "%s", "participantContextId": "%s"}\
                """.formatted(COUNTER_PARTY_ADDRESS, COUNTER_PARTY_ID, ASSET_ID, PARTICIPANT_CONTEXT_ID);
    }
}
