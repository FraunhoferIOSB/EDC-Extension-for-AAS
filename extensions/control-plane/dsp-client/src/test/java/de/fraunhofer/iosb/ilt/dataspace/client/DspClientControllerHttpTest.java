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

import de.fraunhofer.iosb.ilt.dataspace.client.testutils.CatalogFixtures;
import de.fraunhofer.iosb.ilt.dataspace.client.testutils.DspClientTestBase;
import de.fraunhofer.iosb.ilt.dataspace.client.testutils.FakeCatalogService;
import de.fraunhofer.iosb.ilt.dataspace.client.testutils.FakeContractNegotiationService;
import de.fraunhofer.iosb.ilt.dataspace.client.testutils.FakeTransferProcessService;
import de.fraunhofer.iosb.ilt.dataspace.client.testutils.NegotiationFlow;
import okhttp3.Response;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Layer-1 integration tests (plan cases 1.1-1.3, 1.5-1.9) for {@link DspClientController} over real HTTP against the
 * booted lean EDC runtime: controllers, PolicyService, Negotiator, PullTransferInitiator, both production listeners,
 * the in-memory stores/observables and the real JsonLd-expansion plus dsp-catalog-transform-2025 chain are all real.
 * Only the EDC backend service SPIs ({@link CatalogService}, {@link ContractNegotiationService},
 * {@link TransferProcessService}) are replaced by stateful fakes; the fake catalog serves the {@code catalog.json}
 * fixture whose offer carries {@code odrl:assigner provider} (the production shape).
 *
 * <p>This runtime accepts all policies ({@code edc.client.dsp.policy.accepted.all=true}, mirroring the system-test
 * consumer configuration); the {@code false} configuration is covered by
 * {@code DspClientControllerPolicyGatingHttpTest}
 * and the timeout configuration by {@code DspClientControllerTimeoutHttpTest}.
 *
 * <p>The negotiate requests this class sends answer only after the seed events have fired, so tests issue them on an
 * async future ({@link NegotiationFlow}); see the class docs there.
 */
class DspClientControllerHttpTest extends DspClientTestBase {

    private static final String ASSET_ID = "-1379397334";
    private static final String OFFER_ID = "DEFAULT_CONTRACT2:-1379397334:98e4c6c1-3293-46f5-840d-db553b2f1d6f";
    private static final String COUNTER_PARTY_ID = "provider";
    private static final String COUNTER_PARTY_ADDRESS = "http://localhost:13340/dsp/2025-1";
    private static final String DSP_PROTOCOL_2025_1 = "dataspace-protocol-http:2025-1";
    private static final String CONSUMER_ID = "dsp-client-test";
    private static final String OTHER_ASSET_ID = "some-other-asset";
    private static final String EDR_PROXY_URL = "https://provider.example/proxy/edr-1";
    private static final String EDR_AUTH_CODE = "test-auth-code";

    private static final FakeCatalogService CATALOG_SERVICE = new FakeCatalogService();
    private static final FakeContractNegotiationService NEGOTIATION_SERVICE = new FakeContractNegotiationService();
    private static final FakeTransferProcessService TRANSFER_SERVICE = new FakeTransferProcessService();

    private static final int CONTROLLER_RUNTIME_PORT = 8194;


    @RegisterExtension
    static RuntimePerClassExtension runtime = runtimeWithFakes("DspClient Controller", CONTROLLER_RUNTIME_PORT,
            Map.of("edc.client.dsp.policy.accepted.all", "true"));

    @BeforeAll
    static void awaitJetty() {
        awaitRuntimeReadiness(negotiateUrl(CONTROLLER_RUNTIME_PORT));
    }


    @BeforeEach
    void arrangeDefaultFakes() {
        CATALOG_SERVICE.reset();
        NEGOTIATION_SERVICE.reset();
        TRANSFER_SERVICE.reset();
        TRANSFER_SERVICE.respondWithEdr(ASSET_ID, NegotiationFlow.edrDataAddress(EDR_PROXY_URL, EDR_AUTH_CODE));
    }


    @AfterEach
    void cleanNegotiationStore() {
        NegotiationFlow.cleanupNegotiationStore(runtime);
    }


    @Override
    protected int runtimePort() {
        return CONTROLLER_RUNTIME_PORT;
    }


    private static RuntimePerClassExtension runtimeWithFakes(String name, int port, Map<String, String> config) {
        var extension = runtime(name, port, config);
        extension.registerServiceMock(CatalogService.class, CATALOG_SERVICE);
        extension.registerServiceMock(ContractNegotiationService.class, NEGOTIATION_SERVICE);
        extension.registerServiceMock(TransferProcessService.class, TRANSFER_SERVICE);
        return extension;
    }


    /**
     * Case 1.1: happy path with the default (omitted) {@code starttransfer}. Asserts the whole production loop: the
     * catalog request
     * (asset filter criterion, counter-party and DSP 2025/1 protocol), the {@link ContractRequest} built by the
     * controller and Negotiator (offer id/assetId, acceptable offer policy with provider id and USE permission,
     * counter-party address, DSP 2025/1 protocol), the agreement published through the real observable
     * (id provider/consumer/assetId, policy = offer policy, state FINALIZED), and the pull transfer the
     * PullTransferInitiator derives from it (agreement contract id, counter-party address resolved through the real
     * CounterPartyAddressStore, HttpProxy pull transfer type). The response is the EDR data address attached to the
     * started transfer.
     */
    @Test
    void negotiateDefaultsToTransferAndReturnsEdrTest() throws Exception {
        var agreementId = "agreement-" + UUID.randomUUID();
        var responseFuture = CompletableFuture.supplyAsync(() -> postNegotiate(dspClientPullRequest(ASSET_ID), null));

        var capturedRequest = awaitContractRequest();
        var finalized = completeNegotiation(capturedRequest, agreementId);
        var transferRequest = awaitTransferRequest(finalized);
        NegotiationFlow.fireUntil(responseFuture::isDone, "started transfer",
                () -> NegotiationFlow.fireTransferStarted(runtime, TRANSFER_SERVICE.lastTransferProcess(), TRANSFER_SERVICE.lastTransferStartedData()));

        try (Response response = responseFuture.join()) {
            assertEquals(200, response.code(), "negotiate must succeed when offer, negotiation and transfer complete");
            assertEquals("application/json", contentType(response));
            var edr = MAPPER.readTree(body(response));
            assertEquals("HttpProxy", edr.path("properties").path(DataAddress.EDC_DATA_ADDRESS_TYPE_PROPERTY).asText(),
                    "DataAddress.getType is @JsonIgnore and stores the type under its EDC namespace property");
            assertEquals(EDR_PROXY_URL, edr.path("properties").path("proxyUrl").asText());
            assertEquals(EDR_AUTH_CODE, edr.path("properties").path("authCode").asText());
        }

        assertCapturedCatalogRequest(ASSET_ID);
        assertCapturedContractRequest(capturedRequest);
        assertCapturedTransferRequest(transferRequest, agreementId);
        assertEquals(1, NEGOTIATION_SERVICE.callCount());
        assertEquals(1, TRANSFER_SERVICE.callCount());
    }


    /**
     * Case 1.2: explicit {@code starttransfer=false} omits the transfer: the response body is the serialized
     * {@link org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement} and the transfer service is never called.
     */
    @Test
    void startTransferFalseReturnsAgreementWithoutTransferTest() throws Exception {
        var agreementId = "agreement-" + UUID.randomUUID();
        var responseFuture = CompletableFuture.supplyAsync(() -> postNegotiate(dspClientPullRequest(ASSET_ID), false));

        var capturedRequest = awaitContractRequest();
        var finalized = completeNegotiation(capturedRequest, agreementId);
        NegotiationFlow.fireUntil(responseFuture::isDone, "finalized negotiation",
                () -> NegotiationFlow.fireNegotiationFinalized(runtime, finalized));

        try (Response response = responseFuture.join()) {
            assertEquals(200, response.code());
            assertEquals("application/json", contentType(response));
            var agreement = MAPPER.readTree(body(response));
            assertEquals(agreementId, agreement.path("id").asText());
            assertEquals(ASSET_ID, agreement.path("assetId").asText());
            assertEquals(COUNTER_PARTY_ID, agreement.path("providerId").asText());
            assertEquals(ASSET_ID, agreement.path("policy").path("target").asText());
        }

        assertEquals(1, NEGOTIATION_SERVICE.callCount());
        assertEquals(0, TRANSFER_SERVICE.callCount());
        assertNull(TRANSFER_SERVICE.capturedRequest());
    }


    /**
     * Pinned finding (plan assumed {@code startTransfer}): the production {@code @QueryParam} name is all-lowercase
     * {@code starttransfer} and Jersey binds it case-sensitively, so a request with the plan's spelling
     * {@code ?startTransfer=false} falls back to the default {@code true} and starts a transfer against the caller's
     * intent.
     */
    @Test
    void wrongCasedStartTransferParameterIsIgnoredTest() {
        var agreementId = "agreement-" + UUID.randomUUID();
        var responseFuture = CompletableFuture.supplyAsync(
                () -> postNegotiateRawQuery(dspClientPullRequest(ASSET_ID), "startTransfer=false"));

        var capturedRequest = awaitContractRequest();
        var finalized = completeNegotiation(capturedRequest, agreementId);
        var transferRequest = awaitTransferRequest(finalized);
        NegotiationFlow.fireUntil(responseFuture::isDone, "started transfer",
                () -> NegotiationFlow.fireTransferStarted(runtime, TRANSFER_SERVICE.lastTransferProcess(), TRANSFER_SERVICE.lastTransferStartedData()));

        try (Response response = responseFuture.join()) {
            assertEquals(200, response.code(), "wrong-cased parameter is ignored and the flow runs the default: transfer");
            assertEquals(1, TRANSFER_SERVICE.callCount(), "?startTransfer=false is NOT bound to the production starttransfer parameter");
            assertEquals(agreementId, transferRequest.getContractId());
        }
    }


    /**
     * Case 1.3: a transfer that terminates before it started completes the PullTransferInitiator future exceptionally
     * and the controller answers 422 text/plain with the failure detail.
     */
    @Test
    void terminatedTransferIsUnprocessableTest() {
        var agreementId = "agreement-" + UUID.randomUUID();
        var responseFuture = CompletableFuture.supplyAsync(() -> postNegotiate(dspClientPullRequest(ASSET_ID), null));

        var capturedRequest = awaitContractRequest();
        var finalized = completeNegotiation(capturedRequest, agreementId);
        var transferRequest = awaitTransferRequest(finalized);
        assertEquals(agreementId, transferRequest.getContractId());

        var terminated = NegotiationFlow.terminatedTransferProcess(TRANSFER_SERVICE.lastTransferProcess(), "data plane broken");
        NegotiationFlow.fireUntil(responseFuture::isDone, "terminated transfer",
                () -> NegotiationFlow.fireTransferTerminated(runtime, terminated));

        try (Response response = responseFuture.join()) {
            assertEquals(422, response.code(), "a failing transfer is Unprocessable Content, not an InvalidRequest");
            assertEquals("text/plain", contentType(response), "the 422 carries the failure message as plain text");
            var message = body(response);
            assertTrue(message.contains("ExecutionException while waiting for transfer to start"), () -> "unexpected body: " + message);
            assertTrue(message.contains("Transfer process %s TERMINATED".formatted(TRANSFER_SERVICE.lastTransferProcess().getId())),
                    () -> "unexpected body: " + message);
            assertTrue(message.contains("data plane broken"), () -> "unexpected body: " + message);
        }
    }


    /**
     * Updated pin (concurrent production fix): an offer policy without {@code odrl:assigner} used to crash the flow
     * with an NPE before the negotiation was initiated, because the Negotiator stored the counter-party address
     * keyed by the (null) provider id. The current working tree stores the address in the controller, keyed by the
     * request's {@code counterPartyId} and before the catalog is even fetched, so the crash is gone. The missing
     * assigner is still not validated: the negotiation proceeds with a {@code providerId == null} contract request
     * and completes with the usual 200 agreement once the provider finalizes; only a follow-up transfer would fail
     * later, when the address store resolves the agreement's (null) provider id.
     */
    @Test
    void offerWithoutAssignerStillNegotiatesTest() throws Exception {
        CATALOG_SERVICE.respondWith(CatalogFixtures.catalogWithoutAssignerBytes());
        var agreementId = "agreement-" + UUID.randomUUID();
        var responseFuture = CompletableFuture.supplyAsync(() -> postNegotiate(dspClientPullRequest(ASSET_ID), false));

        var capturedRequest = awaitContractRequest();
        assertNull(capturedRequest.getProviderId(), "the missing assigner is still not validated");

        var offerPolicy = capturedRequest.getContractOffer().getPolicy();
        // the provider that made the offer without assigner still signs the agreement with its own id;
        // the response Agreement is not stored with a null provider id.
        var agreement = NegotiationFlow.agreement(agreementId, COUNTER_PARTY_ID, CONSUMER_ID,
                capturedRequest.getContractOffer().getAssetId(), offerPolicy);
        var finalized = NegotiationFlow.finalizedNegotiation(NEGOTIATION_SERVICE.lastNegotiation(), agreement);
        NegotiationFlow.saveNegotiation(runtime, finalized);
        NegotiationFlow.fireUntil(responseFuture::isDone, "finalized negotiation",
                () -> NegotiationFlow.fireNegotiationFinalized(runtime, finalized));

        try (Response response = responseFuture.join()) {
            assertEquals(200, response.code(), "storing the counter-party address by counterPartyId fixed the crash");
            assertEquals("application/json", contentType(response));
            var responseAgreement = MAPPER.readTree(body(response));
            assertEquals(agreementId, responseAgreement.path("id").asText());
            assertEquals(ASSET_ID, responseAgreement.path("assetId").asText());
            assertTrue(responseAgreement.path("policy").path("assigner").isMissingNode() ||
                    responseAgreement.path("policy").path("assigner").isNull(),
                    () -> "unexpected body: " + body(response));
        }

        assertEquals(1, NEGOTIATION_SERVICE.callCount());
        assertEquals(0, TRANSFER_SERVICE.callCount());
    }


    /**
     * Case 1.5: an asset the catalog has no dataset for yields
     * {@link org.eclipse.edc.web.spi.exception.ObjectNotFoundException}
     * mapped to 404.
     */
    @Test
    void assetNotInCatalogIsNotFoundTest() throws Exception {
        CATALOG_SERVICE.respondWith(CatalogFixtures.catalogWithoutDatasetsBytes());

        try (Response response = postNegotiate(dspClientPullRequest(OTHER_ASSET_ID), null)) {
            assertEquals(404, response.code());
            assertEquals("application/json", contentType(response), "404 details are ApiErrorDetail JSON");
            var failureDetail = body(response);
            var errors = MAPPER.readTree(failureDetail);
            assertTrue(errors.isArray());
            assertTrue(errors.get(0).path("message").asText().contains("No datasets were found for asset %s.".formatted(OTHER_ASSET_ID)),
                    () -> "unexpected body: " + failureDetail);
        }

        assertEquals(0, NEGOTIATION_SERVICE.callCount(), "no negotiation is initiated when the asset is not in the catalog");
    }


    /**
     * Case 1.6 (catalog general error): a failing catalog result (e.g. provider unreachable) is surfaced as a 400
     * InvalidRequest carrying the failure detail.
     */
    @Test
    void failingCatalogIsBadRequestTest() throws Exception {
        CATALOG_SERVICE.failWith(StatusResult.failure(ResponseStatus.FATAL_ERROR, "provider catalog blew up"));

        try (Response response = postNegotiate(dspClientPullRequest(ASSET_ID), null)) {
            assertEquals(400, response.code());
            assertEquals("application/json", contentType(response));
            var errors = MAPPER.readTree(body(response));
            assertTrue(errors.isArray());
            var message = errors.get(0).path("message").asText();
            assertTrue(message.contains("Failed fetching catalog"), () -> "unexpected body: " + message);
            assertTrue(message.contains("FATAL_ERROR: provider catalog blew up"), () -> "unexpected body: " + message);
        }

        assertEquals(0, NEGOTIATION_SERVICE.callCount());
    }


    /**
     * Case 1.6 (duplicate datasets): a catalog that lists the asset twice is ambiguous and yields
     * {@link org.eclipse.edc.web.spi.exception.ObjectConflictException} mapped to 409.
     */
    @Test
    void duplicateDatasetsIsConflictTest() throws Exception {
        CATALOG_SERVICE.respondWith(CatalogFixtures.catalogWithDuplicateDatasetsBytes());

        try (Response response = postNegotiate(dspClientPullRequest(ASSET_ID), null)) {
            assertEquals(409, response.code());
            assertEquals("application/json", contentType(response));
            var errors = MAPPER.readTree(body(response));
            assertTrue(errors.isArray());
            var message = errors.get(0).path("message").asText();
            assertTrue(message.contains("Multiple datasets were found for asset %s.".formatted(ASSET_ID)),
                    () -> "unexpected body: " + message);
        }

        assertEquals(0, NEGOTIATION_SERVICE.callCount());
    }


    /**
     * Case 1.8 (pinned, plan-confirmed): an empty request body is injected as a null DTO by Jersey and the controller
     * dereferences it unguarded, so the request crashes with a bare 500 response instead of the 4xx
     * {@code MISSING_REQUEST_BODY_MESSAGE} the PolicyController reports for the same situation. The fallback mapper
     * produces an entity-less response, which the embedded Jetty fills with its HTML error page. Documented
     * validation-gap finding.
     */
    @Test
    void emptyBodyCrashesWithServerErrorTest() {
        try (Response response = postNegotiateEmptyBody()) {
            assertEquals(500, response.code(), "Missing body: the unguarded DTO dereference crashes with a bare 500");
            var failureDetail = body(response);
            assertTrue(failureDetail.startsWith("<html>"),
                    () -> "entity-less 500 gets the container's default HTML error page: " + failureDetail);
            assertEquals(0, CATALOG_SERVICE.requestCount(), "the crash happens before any production call");
        }
    }


    /**
     * Case 1.8 (pinned): a body missing the required {@code assetId} fails the {@code PullRequestDTO} record
     * constructor and surfaces as 400 text/plain through the Jackson binding exceptions. The pinned body shows the
     * usability gap that the mapper does NOT name the missing field: the NPE is wrapped UNNAMED (unlike
     * PolicyController, which reports explicit messages).
     */
    @Test
    void missingAssetIdIsRejectedAsBadRequestTest() {
        try (Response response = postNegotiate(pullRequestJsonWithout("assetId"), null)) {
            assertEquals(400, response.code(), "the record compact constructor accepts no null assetId");
            assertEquals("text/plain", contentType(response), "Jackson binding failures are mapped to text/plain 400");
            var failureDetail = body(response).toLowerCase();
            assertTrue(failureDetail.contains("cannot construct instance of"), () -> "unexpected body: " + failureDetail);
            assertTrue(failureDetail.contains("pullrequestdto"), () -> "unexpected body: " + failureDetail);
            assertTrue(failureDetail.contains("nullpointerexception"), () -> "unexpected body: " + failureDetail);
        }
    }


    /**
     * Case 1.8 (pinned): a body missing the required {@code counterPartyId} fails the same way, likewise without the
     * field being named in the response.
     */
    @Test
    void missingCounterPartyIdIsRejectedAsBadRequestTest() {
        try (Response response = postNegotiate(pullRequestJsonWithout("counterPartyId"), null)) {
            assertEquals(400, response.code());
            assertEquals("text/plain", contentType(response), "Jackson binding failures are mapped to text/plain 400");
            var failureDetail = body(response).toLowerCase();
            assertTrue(failureDetail.contains("cannot construct instance of"), () -> "unexpected body: " + failureDetail);
            assertTrue(failureDetail.contains("pullrequestdto"), () -> "unexpected body: " + failureDetail);
            assertTrue(failureDetail.contains("nullpointerexception"), () -> "unexpected body: " + failureDetail);
        }
    }


    /**
     * Case 1.9 (pinned, plan-deviation): a request missing the {@code x-api-key} header is answered with 401, not the
     * 403 the plan expected: the token-based auth service raises AuthenticationFailedException which the API exception
     * mapper resolves to UNAUTHORIZED, and the response never reaches the controller.
     */
    @Test
    void missingApiKeyIsUnauthorizedTest() {
        try (Response response = postNegotiateWithoutAuth(dspClientPullRequest(ASSET_ID), null)) {
            assertEquals(401, response.code(), "AuthenticationFailedException maps to 401, not 403");
        }

        assertEquals(0, CATALOG_SERVICE.requestCount(), "the request must be rejected before it reaches the controller");
    }


    private static String dspClientPullRequest(String assetId) {
        return """
                {"counterPartyAddress": "%s", "counterPartyId": "%s", "assetId": "%s", "participantContextId": "%s"}\
                """.formatted(COUNTER_PARTY_ADDRESS, COUNTER_PARTY_ID, assetId, PARTICIPANT_CONTEXT_ID);
    }


    private static String pullRequestJsonWithout(String missingField) {
        var json = new StringBuilder("{");
        if (!"counterPartyAddress".equals(missingField)) {
            json.append("\"counterPartyAddress\": \"").append(COUNTER_PARTY_ADDRESS).append("\", ");
        }
        if (!"counterPartyId".equals(missingField)) {
            json.append("\"counterPartyId\": \"").append(COUNTER_PARTY_ID).append("\", ");
        }
        if (!"assetId".equals(missingField)) {
            json.append("\"assetId\": \"").append(ASSET_ID).append("\", ");
        }
        json.append("\"participantContextId\": \"").append(PARTICIPANT_CONTEXT_ID).append("\"}");
        return json.toString();
    }


    private ContractRequest awaitContractRequest() {
        return NegotiationFlow.awaitValue(NEGOTIATION_SERVICE::capturedRequest, "contract request");
    }


    /**
     * Builds and saves the finalized negotiation the provider would publish: fresh agreement on the captured offer
     * policy with the assigner as provider, mirroring the fake negotiation's identifiers.
     */
    private ContractNegotiation completeNegotiation(ContractRequest capturedRequest, String agreementId) {
        var offerPolicy = capturedRequest.getContractOffer().getPolicy();
        var agreement = NegotiationFlow.agreement(agreementId, offerPolicy.getAssigner(), CONSUMER_ID,
                capturedRequest.getContractOffer().getAssetId(), offerPolicy);
        var finalized = NegotiationFlow.finalizedNegotiation(NEGOTIATION_SERVICE.lastNegotiation(), agreement);
        NegotiationFlow.saveNegotiation(runtime, finalized);
        return finalized;
    }


    /**
     * Fires the finalized negotiation until the transfer fake publishes the request.
     * The fake publishes its process first; the production transfer future may be registered later.
     */
    private TransferRequest awaitTransferRequest(ContractNegotiation finalized) {
        NegotiationFlow.fireUntil(() -> TRANSFER_SERVICE.capturedRequest() != null, "negotiation into transfer",
                () -> NegotiationFlow.fireNegotiationFinalized(runtime, finalized));
        return TRANSFER_SERVICE.capturedRequest();
    }


    private void assertCapturedCatalogRequest(String assetId) {
        var catalogRequest = CATALOG_SERVICE.capturedRequest();
        assertEquals(COUNTER_PARTY_ID, catalogRequest.counterPartyId());
        assertEquals(COUNTER_PARTY_ADDRESS, catalogRequest.counterPartyAddress());
        assertEquals(DSP_PROTOCOL_2025_1, catalogRequest.protocol());
        var filter = catalogRequest.querySpec().getFilterExpression();
        assertEquals(1, filter.size(), "the catalog request filters by the requested asset id");
        var criterion = filter.getFirst();
        assertEquals(Asset.PROPERTY_ID, criterion.getOperandLeft());
        assertEquals("=", criterion.getOperator());
        assertEquals(assetId, criterion.getOperandRight());
    }


    private void assertCapturedContractRequest(ContractRequest capturedRequest) {
        assertEquals(DSP_PROTOCOL_2025_1, capturedRequest.getProtocol());
        assertEquals(COUNTER_PARTY_ADDRESS, capturedRequest.getCounterPartyAddress());
        assertEquals(COUNTER_PARTY_ID, capturedRequest.getProviderId(), "provider id is the acceptable offer policy assigner");
        ContractOffer offer = capturedRequest.getContractOffer();
        assertEquals(OFFER_ID, offer.getId());
        assertEquals(ASSET_ID, offer.getAssetId());
        var policy = offer.getPolicy();
        assertEquals(ASSET_ID, policy.getTarget(), "the acceptable offer policy target is overridden with the asset id");
        assertEquals(COUNTER_PARTY_ID, policy.getAssigner());
        assertEquals(1, policy.getPermissions().size());
        assertEquals("USE", policy.getPermissions().getFirst().getAction().getType());
        assertTrue(policy.getProhibitions().isEmpty());
        assertTrue(policy.getObligations().isEmpty());
    }


    private void assertCapturedTransferRequest(TransferRequest transferRequest, String agreementId) {
        assertEquals(agreementId, transferRequest.getContractId());
        assertEquals(COUNTER_PARTY_ADDRESS, transferRequest.getCounterPartyAddress(),
                "counter-party address was resolved through the real CounterPartyAddressStore");
        assertEquals("ProxyHttpData-PULL", transferRequest.getTransferType());
        assertEquals(DSP_PROTOCOL_2025_1, transferRequest.getProtocol());
    }
}
