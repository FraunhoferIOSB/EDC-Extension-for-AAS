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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import de.fraunhofer.iosb.ilt.dataspace.client.testutils.CatalogFixtures;
import de.fraunhofer.iosb.ilt.dataspace.client.testutils.DspClientTestBase;
import de.fraunhofer.iosb.ilt.dataspace.client.testutils.FakeContractNegotiationService;
import de.fraunhofer.iosb.ilt.dataspace.client.testutils.NegotiationFlow;
import okhttp3.Response;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Layer-3 integration tests (plan cases 3.1, 3.3 and 3.4) for the full dsp-client catalog flow against a simulated
 * DSP provider: the runtime keeps the REAL control-plane catalog machinery (aggregate-services CatalogServiceImpl,
 * DSP-2025 request dispatch through the real {@code PostDspHttpRequestFactory}, WireMock as the provider, real JsonLd
 * expansion and dsp-catalog-transform-2025 chain, real stores and observables) and only the
 * {@link ContractNegotiationService} is faked, driving the real {@code ContractNegotiationObservable} with a finalized
 * agreement (non-null provider id resolved from the offer assigner).
 *
 * <p>This runtime accepts all policies ({@code edc.client.dsp.policy.accepted.all=true}, mirroring the system-test
 * consumer configuration); the {@code false} default is covered by {@code DspClientFullFlowPolicyGatingHttpTest}
 * (plan case 3.2).
 *
 * <p>The plan's single-class idea for Layer 3 was split into two classes because the accept-all configuration is
 * bound to the runtime (Layer-1 finding: Layer 1 needed the same split).
 *
 * <p>Pinned wire contract (case 3.1): the counter-party address, including the DSP version segment the client was
 * given ({@code /2025/1}), is the request base path — the dispatcher appends only {@code /catalog/request}
 * ({@code CatalogApiPaths}: no version segment is added by the dispatcher). To keep the 500-retry case fast,
 * {@code edc.core.retry.retries.max=2} (production default 5 with 500ms..10s exponential backoff): a persistent
 * provider 500 therefore surfaces as {@code 1 + retries} catalog requests, {@code 3} with this runtime's config.
 */
class DspClientFullFlowHttpTest extends DspClientTestBase {

    private static final int FULL_FLOW_PORT = FULL_FLOW_RUNTIME_PORT;
    private static final String ASSET_ID = "-1379397334";
    private static final String OFFER_ID = "DEFAULT_CONTRACT2:-1379397334:98e4c6c1-3293-46f5-840d-db553b2f1d6f";
    private static final String COUNTER_PARTY_ID = "provider";
    private static final String CONSUMER_ID = PARTICIPANT_CONTEXT_ID;
    private static final String OTHER_ASSET_ID = "999999999";
    private static final String DSP_VERSION_PATH = "/2025/1";
    private static final String CATALOG_REQUEST_PATH = DSP_VERSION_PATH + "/catalog/request";
    private static final String DSP_PROTOCOL_2025_1 = "dataspace-protocol-http:2025-1";
    private static final int RETRIES_MAX = 2;
    private static final int CATALOG_REQUEST_ATTEMPTS = 1 + RETRIES_MAX;

    private static final FakeContractNegotiationService NEGOTIATION_SERVICE = new FakeContractNegotiationService();

    @RegisterExtension
    static WireMockExtension provider = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @RegisterExtension
    static RuntimePerClassExtension runtime = fullFlowRuntime(FULL_FLOW_PORT, Map.of(
            "edc.client.dsp.policy.accepted.all", "true"));

    @BeforeAll
    static void awaitJetty() {
        awaitRuntimeReadiness(negotiateUrl(FULL_FLOW_PORT));
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
        NegotiationFlow.cleanupNegotiationStore(runtime);
    }


    @Override
    protected int runtimePort() {
        return FULL_FLOW_PORT;
    }


    /**
     * Case 3.1 (happy path): the negotiate request drives the real catalog dispatch against the simulated provider,
     * whose stub serves the {@code catalog.json} fixture offer. The WireMock request pins the wire contract of the
     * real dispatcher: path = counter-party address (with the version segment as supplied) + {@code /catalog/request},
     * method POST, {@code Content-Type: application/json}, a mock-IAM {@code Authorization} header and the compacted
     * JSON-LD body carrying the asset-equals filter the PolicyService built. The faked negotiation is finalized with an
     * agreement, so the controller answers 200 with the agreement JSON and the transfer leg is skipped
     * ({@code starttransfer=false}).
     */
    @Test
    void catalogRequestReachesProviderAndNegotiationYieldsAgreementTest() throws Exception {
        var agreementId = "agreement-" + UUID.randomUUID();
        var responseFuture = CompletableFuture.supplyAsync(() -> postNegotiate(dspClientPullRequest(ASSET_ID), false));

        var capturedRequest = awaitContractRequest();
        var offerPolicy = capturedRequest.getContractOffer().getPolicy();
        assertEquals(COUNTER_PARTY_ID, offerPolicy.getAssigner(), "the real expansion resolves the offer assigner as provider id");
        var finalized = completeNegotiation(capturedRequest, agreementId);
        NegotiationFlow.fireUntil(responseFuture::isDone, "finalized negotiation",
                () -> NegotiationFlow.fireNegotiationFinalized(runtime, finalized));

        try (Response response = responseFuture.join()) {
            assertEquals(200, response.code(), "negotiate must succeed when the provider serves the fixture catalog");
            assertEquals("application/json", contentType(response));
            var agreement = MAPPER.readTree(body(response));
            assertEquals(agreementId, agreement.path("id").asText());
            assertEquals(ASSET_ID, agreement.path("assetId").asText());
            assertEquals(COUNTER_PARTY_ID, agreement.path("providerId").asText());
            assertEquals(ASSET_ID, agreement.path("policy").path("target").asText());
        }

        assertEquals(1, NEGOTIATION_SERVICE.callCount());
        assertProviderCatalogRequest(ASSET_ID);
        assertEquals(DSP_PROTOCOL_2025_1, capturedRequest.getProtocol());
        assertEquals(counterPartyAddress(), capturedRequest.getCounterPartyAddress());
        assertEquals(COUNTER_PARTY_ID, capturedRequest.getProviderId());
        assertEquals(OFFER_ID, capturedRequest.getContractOffer().getId());
        assertEquals(ASSET_ID, capturedRequest.getContractOffer().getAssetId());
        assertEquals(ASSET_ID, offerPolicy.getTarget());
    }


    /**
     * Case 3.3a: the provider answers with a valid catalog that contains no dataset ({@code catalog-empty.json}); the
     * dataset lookup fails with {@code ObjectNotFoundException} and the controller answers 404. No negotiation is
     * initiated and exactly one catalog request reaches the provider.
     */
    @Test
    void emptyProviderCatalogIsNotFoundTest() throws Exception {
        stubCatalogResponse(resourceBytes("catalog-empty.json"));

        try (Response response = postNegotiate(dspClientPullRequest(ASSET_ID), false)) {
            assertEquals(404, response.code(), "a catalog without datasets makes the asset lookup not-found");
            assertEquals("application/json", contentType(response));
            var errors = MAPPER.readTree(body(response));
            assertTrue(errors.isArray());
            var message = errors.get(0).path("message").asText();
            assertTrue(message.contains("No datasets were found for asset %s.".formatted(ASSET_ID)), () -> "unexpected body: " + message);
        }

        assertEquals(0, NEGOTIATION_SERVICE.callCount(), "no negotiation is initiated without a dataset");
        provider.verify(1, postRequestedFor(urlPathEqualTo(CATALOG_REQUEST_PATH)));
    }


    /**
     * Case 3.3a (mismatch flavor, pinned): the provider returns a catalog whose only dataset carries ANOTHER asset
     * id, i.e. it ignores the requested asset filter. The production flow does not re-check the dataset's asset id
     * against the requested one after fetching (PolicyService only checks for absent or ambiguous datasets), so the
     * request is negotiated anyway — the offer built from the mismatched dataset is turned into an agreement for the
     * REQUESTED asset id. Follow-up finding: the consumer-side flow trusts the provider to apply the filter.
     */
    @Test
    void providerCatalogIgnoringTheAssetFilterIsStillNegotiatedTest() throws Exception {
        stubCatalogResponse(catalogFixtureWithAssetId(OTHER_ASSET_ID));
        var agreementId = "agreement-" + UUID.randomUUID();
        var responseFuture = CompletableFuture.supplyAsync(() -> postNegotiate(dspClientPullRequest(ASSET_ID), false));

        var capturedRequest = awaitContractRequest();
        var finalized = completeNegotiation(capturedRequest, agreementId);
        NegotiationFlow.fireUntil(responseFuture::isDone, "finalized negotiation",
                () -> NegotiationFlow.fireNegotiationFinalized(runtime, finalized));

        try (Response response = responseFuture.join()) {
            assertEquals(200, response.code(), "the mismatched dataset is not rejected: the client trusts the provider-side filter");
            var agreement = MAPPER.readTree(body(response));
            assertEquals(agreementId, agreement.path("id").asText());
            assertEquals(ASSET_ID, agreement.path("assetId").asText(), "the agreement is built for the REQUESTED asset");
        }

        assertEquals(1, NEGOTIATION_SERVICE.callCount());
        assertEquals(ASSET_ID, capturedRequest.getContractOffer().getAssetId(), "the requested asset is negotiated with the mismatched dataset's offer");
    }


    /**
     * Case 3.3b (invalid JSON-LD): a 200 response whose JSON-LD cannot be expanded (an unreachable remote
     * {@code @context}, while offline expansion is enabled by default) fails in the real JsonLd expansion and the
     * controller answers 400 carrying the expansion failure detail.
     */
    @Test
    void invalidJsonLdFromProviderIsBadRequestTest() throws Exception {
        stubResponse(200, "application/json",
                "{\"@context\": \"http://localhost:1/unresolvable-context.jsonld\", \"@type\": \"dcat:Catalog\"}");

        try (Response response = postNegotiate(dspClientPullRequest(ASSET_ID), false)) {
            assertEquals(400, response.code(), "an unexpandable catalog body is an InvalidRequest");
            assertEquals("application/json", contentType(response));
            var errors = MAPPER.readTree(body(response));
            assertTrue(errors.isArray());
            var message = errors.get(0).path("message").asText();
            assertTrue(message.contains("Failed fetching catalog"), () -> "unexpected body: " + message);
        }

        assertEquals(0, NEGOTIATION_SERVICE.callCount());
    }


    /**
     * Case 3.3b (non-JSON payload, pinned): a 200 response that is not JSON at all makes {@code PolicyService}'s
     * {@code Json.createReader(...).readObject()} throw a runtime exception, which the fallback mapper surfaces as an
     * entity-less bare 500 — the embedded Jetty fills in its default HTML error page (same shape as the Layer-1
     * entity-less 5xx findings), not the plan's 400.
     */
    @Test
    void nonJsonPayloadFromProviderCrashesWithServerErrorTest() {
        stubResponse(200, "text/plain", "this is not a json-ld catalog");

        try (Response response = postNegotiate(dspClientPullRequest(ASSET_ID), false)) {
            assertEquals(500, response.code(), "the unguaraded JSON parse of the catalog body crashes with a bare 500");
            var failureDetail = body(response);
            assertTrue(failureDetail.startsWith("<html>"),
                    () -> "entity-less 500 gets the container's default HTML error page: " + failureDetail);
        }

        assertEquals(0, NEGOTIATION_SERVICE.callCount());
    }


    /**
     * Case 3.4: a failing provider (500 on the catalog request) surfaces as 400 carrying the failure detail of the
     * last HTTP attempt. The EdcHttpClient fallback {@code retryWhenStatusNot2xxOr4xx} marks 5xx responses for retry,
     * so with this runtime's {@code edc.core.retry.retries.max=2} the provider sees {@code 1 + retries} = 3 requests
     * (6 with the production default of 5); after the retries are exhausted the retry-limit exception is wrapped into
     * the "Failed fetching catalog" detail.
     */
    @Test
    void providerErrorIsBadRequestWithFailureDetailTest() throws Exception {
        stubResponse(500, "text/plain", "provider catalog exploded");

        try (Response response = postNegotiate(dspClientPullRequest(ASSET_ID), false)) {
            assertEquals(400, response.code(), "a failing provider surfaces as InvalidRequest like every catalog failure");
            assertEquals("application/json", contentType(response));
            var errors = MAPPER.readTree(body(response));
            assertTrue(errors.isArray());
            assertEquals("InvalidRequest", errors.get(0).path("type").asText());
            var message = errors.get(0).path("message").asText();
            assertTrue(message.contains("Failed fetching catalog"), () -> "unexpected body: " + message);
            assertTrue(message.contains("500"), () -> "unexpected body: " + message);
            assertTrue(message.contains("provider catalog exploded"), () -> "unexpected body: " + message);
        }

        assertEquals(0, NEGOTIATION_SERVICE.callCount());
        provider.verify(CATALOG_REQUEST_ATTEMPTS, postRequestedFor(urlPathEqualTo(CATALOG_REQUEST_PATH)));
    }


    /**
     * Pins the request the real DSP-2025 dispatcher sent to the provider: full URL (counter-party address with its
     * version segment + {@code /catalog/request}), method, content type, mock-IAM authorization and the compacted
     * JSON-LD body carrying the asset-equals filter.
     */
    private void assertProviderCatalogRequest(String assetId) throws IOException {
        var requests = provider.findAll(postRequestedFor(urlPathEqualTo(CATALOG_REQUEST_PATH)));
        assertEquals(1, requests.size(), "exactly one catalog request reaches the provider");
        var request = requests.get(0);

        assertEquals("POST", request.getMethod().getName());
        assertEquals(counterPartyAddress() + "/catalog/request", request.getAbsoluteUrl(),
                "the version segment is part of the counter-party address; the dispatcher appends only /catalog/request");
        assertEquals("application/json; charset=utf-8", request.getHeader("Content-Type"),
                "OkHttp appends the request body charset to the factory's application/json content type");
        var authorization = request.getHeader("Authorization");
        assertTrue(authorization != null && !authorization.isBlank(),
                "the dispatcher obtains mock-IAM credentials and sends them as Authorization header");
        assertTrue(authorization.contains("\"clientId\":\"dsp-client-test\""), "unexpected Authorization token: " + authorization);
        assertTrue(authorization.contains("\"audience\":\"" + counterPartyAddress() + "\""),
                "the mock IAM resolves the audience as the counter-party address: " + authorization);

        var bodyJson = MAPPER.readTree(request.getBodyAsString());
        assertEquals("CatalogRequestMessage", bodyJson.path("@type").asText(), "request body: " + request.getBodyAsString());
        var filterContext = bodyJson.path("@context");
        assertTrue(filterContext.isArray() && filterContext.get(0).asText().equals("https://w3id.org/dspace/2025/1/context.jsonld"),
                "the body is compacted with the DSP 2025/1 scope context: " + request.getBodyAsString());
        var querySpec = bodyJson.path("filter").get(0);
        assertEquals("QuerySpec", querySpec.path("@type").asText(), "request body: " + request.getBodyAsString());
        assertEquals(0, querySpec.path("offset").asInt(), "request body: " + request.getBodyAsString());
        assertEquals(50, querySpec.path("limit").asInt(), "request body: " + request.getBodyAsString());
        assertEquals("ASC", querySpec.path("sortOrder").asText(), "request body: " + request.getBodyAsString());
        var criterion = querySpec.path("filterExpression").get(0);
        assertEquals("Criterion", criterion.path("@type").asText(), "request body: " + request.getBodyAsString());
        assertEquals(Asset.PROPERTY_ID, criterion.path("operandLeft").asText(), "request body: " + request.getBodyAsString());
        assertEquals("=", criterion.path("operator").asText(), "request body: " + request.getBodyAsString());
        assertEquals(assetId, criterion.path("operandRight").asText(), "request body: " + request.getBodyAsString());
    }


    private ContractRequest awaitContractRequest() {
        return NegotiationFlow.awaitValue(NEGOTIATION_SERVICE::capturedRequest, "contract request");
    }


    /**
     * Builds and saves the finalized negotiation the provider would publish: fresh agreement on the captured offer
     * policy with the offer's real assigner as provider, mirroring the fake negotiation's identifiers.
     */
    private ContractNegotiation completeNegotiation(ContractRequest capturedRequest, String agreementId) {
        var offerPolicy = capturedRequest.getContractOffer().getPolicy();
        var agreement = NegotiationFlow.agreement(agreementId, offerPolicy.getAssigner(), CONSUMER_ID,
                capturedRequest.getContractOffer().getAssetId(), offerPolicy);
        var finalized = NegotiationFlow.finalizedNegotiation(NEGOTIATION_SERVICE.lastNegotiation(), agreement);
        NegotiationFlow.saveNegotiation(runtime, finalized);
        return finalized;
    }


    private String counterPartyAddress() {
        return provider.baseUrl() + DSP_VERSION_PATH;
    }


    private String dspClientPullRequest(String assetId) {
        return """
                {"counterPartyAddress": "%s", "counterPartyId": "%s", "assetId": "%s", "participantContextId": "%s"}\
                """.formatted(counterPartyAddress(), COUNTER_PARTY_ID, assetId, PARTICIPANT_CONTEXT_ID);
    }


    private void stubCatalogResponse(byte[] catalogBytes) {
        stubResponse(200, "application/json", catalogBytes);
    }


    private void stubResponse(int status, String contentType, byte[] body) {
        provider.stubFor(post(urlPathEqualTo(CATALOG_REQUEST_PATH))
                .willReturn(aResponse()
                        .withStatus(status)
                        .withHeader("Content-Type", contentType)
                        .withBody(body)));
    }


    private void stubResponse(int status, String contentType, String body) {
        stubResponse(status, contentType, body.getBytes(StandardCharsets.UTF_8));
    }


    /**
     * Derives the fixture catalog with the dataset's {@code edc:id} replaced, i.e. a provider that lists a dataset for
     * another asset and ignores the requested asset filter.
     */
    private static byte[] catalogFixtureWithAssetId(String assetId) {
        try {
            var catalog = (ObjectNode) MAPPER.readTree(CatalogFixtures.catalogBytes());
            ((ObjectNode) catalog.path("dcat:dataset")).put("edc:id", assetId);
            return MAPPER.writeValueAsBytes(catalog);
        }
        catch (IOException e) {
            throw new UncheckedIOException("Failed deriving catalog fixture", e);
        }
    }


    private static byte[] resourceBytes(String name) {
        try (var stream = DspClientFullFlowHttpTest.class.getClassLoader().getResourceAsStream(name)) {
            return Objects.requireNonNull(stream, "missing test resource " + name).readAllBytes();
        }
        catch (IOException e) {
            throw new UncheckedIOException("Failed reading test resource " + name, e);
        }
    }


    /**
     * The full-flow runtime: real control-plane services from the base-bom classpath, only the
     * {@code ContractNegotiationService} is replaced by a fake (mocks registered pre-boot always win). The retry
     * policy is capped to keep the 500-retry case fast ({@code edc.core.retry.retries.max}); {@code edc.participant.id}
     * must be set explicitly for the mock IAM to mint tokens offline.
     */
    private static RuntimePerClassExtension fullFlowRuntime(int port, Map<String, String> additionalConfig) {
        var config = new HashMap<String, String>(additionalConfig);
        config.put("edc.participant.id", PARTICIPANT_CONTEXT_ID);
        config.put("edc.core.retry.retries.max", String.valueOf(RETRIES_MAX));
        config.put("edc.core.retry.backoff.min", "5");
        config.put("edc.core.retry.backoff.max", "25");
        var extension = runtime("DspClient Full Flow", port, Map.copyOf(config));
        extension.registerServiceMock(ContractNegotiationService.class, NEGOTIATION_SERVICE);
        return extension;
    }
}
