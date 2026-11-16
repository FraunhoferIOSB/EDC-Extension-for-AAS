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
package de.fraunhofer.iosb.ilt.dataspace.client.testutils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;


/**
 * Base class for integration tests that exercise the dsp-client controllers over real HTTP against a booted
 * {@link EmbeddedRuntime}.
 *
 * <p>Subclasses register the runtime as a class-scoped JUnit extension and state the port it serves the default API
 * context on:
 *
 * <pre>
 * {@code
 * &#64;RegisterExtension
 * static RuntimePerClassExtension runtime = leanRuntime();
 *
 * @Override
 * protected int runtimePort() {
 *     return LEAN_RUNTIME_PORT;
 * }
 * }
 * </pre>
 *
 * <p>Extension point: additional runtimes (e.g. the full-flow runtime for Layer-3 tests) are built with
 * {@link #runtime(String, int, Map)} using their own name, port (e.g. {@link #FULL_FLOW_RUNTIME_PORT}) and additional
 * configuration, so this base class does not need to be modified. Method-level helpers resolve their URLs from
 * {@link #runtimePort()}.
 */
public abstract class DspClientTestBase {

    public static final String X_API_KEY = "x-api-key";
    protected static final String AUTH_KEY = "dsp-client-test-key";
    protected static final String PARTICIPANT_CONTEXT_ID = "dsp-client-test";
    protected static final int LEAN_RUNTIME_PORT = 8199;
    protected static final int FULL_FLOW_RUNTIME_PORT = 8198;
    private static final int MANAGEMENT_CONTEXT_PORT_OFFSET = 40;
    private static final int CONTROL_CONTEXT_PORT_OFFSET = 50;
    private static final int PROTOCOL_CONTEXT_PORT_OFFSET = 60;
    protected static final String API_PATH = "api";
    protected static final String NEGOTIATE_PATH = "v1/dspclient/negotiate";
    protected static final String ACCEPTED_POLICIES_PATH = "v1/dspclient/policydefinitions/accepted";

    protected static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");
    private static final OkHttpClient CLIENT = new OkHttpClient();


    /**
     * Returns the port on which the runtime under test serves the default API context.
     */
    protected abstract int runtimePort();


    /**
     * Creates the lean runtime (Layers 1 and 2) that boots the whole test runtime classpath with the dsp-client
     * extension and its real wiring, serving the default API context on {@link #LEAN_RUNTIME_PORT}.
     */
    protected static RuntimePerClassExtension leanRuntime() {
        return runtime("DspClient Lean", LEAN_RUNTIME_PORT, Map.of());
    }


    /**
     * Creates a runtime with the given name, default API port and additional configuration. The configuration always
     * contains the http/web, authentication and participant context settings required by the dsp-client tests;
     * edc.client.dsp.* timeout properties are deliberately not set (production defaults apply).
     *
     * <p>The auto-loaded control-plane web contexts (management, control, protocol) would otherwise fall back to
     * their shared default ports (8181/9191/8282); pinning them to per-class values derived from the default API
     * port lets every class keep its private Jetty, so one class's runtime never conflicts with another's (failed
     * boots leak open connectors) or with connectors running elsewhere on the machine.
     */
    protected static RuntimePerClassExtension runtime(String name, int port, Map<String, String> additionalConfig) {
        return new RuntimePerClassExtension(new EmbeddedRuntime(name)
                .configurationProvider(() -> ConfigFactory.fromMap(runtimeConfiguration(port, additionalConfig))));
    }


    private static Map<String, String> runtimeConfiguration(int port, Map<String, String> additionalConfig) {
        var config = new HashMap<String, String>();
        config.put("web.http.port", String.valueOf(port));
        config.put("web.http.path", "/" + API_PATH);
        config.put("web.http.management.port", String.valueOf(port + MANAGEMENT_CONTEXT_PORT_OFFSET));
        config.put("web.http.control.port", String.valueOf(port + CONTROL_CONTEXT_PORT_OFFSET));
        config.put("web.http.protocol.port", String.valueOf(port + PROTOCOL_CONTEXT_PORT_OFFSET));
        config.put("web.http.default.auth.type", "tokenbased");
        config.put("web.http.default.auth.key", AUTH_KEY);
        config.put("edc.participant.context.id", PARTICIPANT_CONTEXT_ID);
        config.putAll(additionalConfig);
        return config;
    }


    protected static String apiRootUrl(int port) {
        return "http://localhost:" + port + "/" + API_PATH;
    }


    protected static String negotiateUrl(int port) {
        return apiRootUrl(port) + "/" + NEGOTIATE_PATH;
    }


    protected static String acceptedPolicyDefinitionsUrl(int port) {
        return apiRootUrl(port) + "/" + ACCEPTED_POLICIES_PATH;
    }


    protected String negotiateUrl() {
        return negotiateUrl(runtimePort());
    }


    protected String acceptedPolicyDefinitionsUrl() {
        return acceptedPolicyDefinitionsUrl(runtimePort());
    }


    protected String apiRoot() {
        return apiRootUrl(runtimePort());
    }


    protected Request.Builder request(String url) {
        return new Request.Builder().url(url);
    }


    protected Request.Builder authedRequest(String url, String... additionalHeaders) {
        var builder = request(url).header(X_API_KEY, AUTH_KEY);
        for (var i = 0; i < additionalHeaders.length; i += 2) {
            builder = builder.header(additionalHeaders[i], additionalHeaders[i + 1]);
        }
        return builder;
    }


    protected Response execute(Request request) {
        try {
            return CLIENT.newCall(request).execute();
        }
        catch (IOException e) {
            throw new AssertionError("HTTP request to " + request.url() + " failed", e);
        }
    }


    protected static String body(Response response) {
        try {
            return response.body().string();
        }
        catch (IOException e) {
            throw new AssertionError("Failed reading response body of " + response.request().url(), e);
        }
    }


    protected static String contentType(Response response) {
        var mediaType = response.body().contentType();
        return mediaType == null ? null : mediaType.toString();
    }


    protected Response postNegotiate(String jsonBody, Boolean startTransfer) {
        var url = negotiateUrl();
        if (startTransfer != null) {
            url = url + "?starttransfer=" + startTransfer;
        }
        return execute(authedRequest(url).post(jsonBody(jsonBody)).build());
    }


    /**
     * POSTs a zero-length application/json body to the negotiate endpoint. Jersey injects no entity for an empty
     * request body, which lets the tests pin how the controller handles a missing {@code PullRequestDTO}.
     */
    protected Response postNegotiateEmptyBody() {
        return execute(authedRequest(negotiateUrl()).post(RequestBody.create(new byte[0], JSON_MEDIA_TYPE)).build());
    }


    /**
     * POSTs a negotiate request without the {@code x-api-key} header to pin the authentication failure mapping.
     *
     * <p>The query parameter is the production {@code starttransfer} (all lowercase): Jersey binds {@code @QueryParam}
     * case-sensitively, so a wrong-cased {@code startTransfer} parameter would silently fall back to the default.
     */
    protected Response postNegotiateWithoutAuth(String jsonBody, Boolean startTransfer) {
        var url = negotiateUrl();
        if (startTransfer != null) {
            url = url + "?starttransfer=" + startTransfer;
        }
        return execute(request(url).post(jsonBody(jsonBody)).build());
    }


    /**
     * POSTs a negotiate request with the given raw query string, so tests can pin how the production
     * {@code starttransfer} parameter reacts to a wrong-cased name ({@code startTransfer}).
     */
    protected Response postNegotiateRawQuery(String jsonBody, String rawQuery) {
        var url = rawQuery == null || rawQuery.isBlank() ? negotiateUrl() : negotiateUrl() + "?" + rawQuery;
        return execute(authedRequest(url).post(jsonBody(jsonBody)).build());
    }


    protected Response getAcceptedPolicyDefinitions() {
        return execute(authedRequest(acceptedPolicyDefinitionsUrl()).get().build());
    }


    protected Response postAcceptedPolicyDefinition(String json, String mediaType) {
        var requestBody = mediaType == null ? RequestBody.create(new byte[0]) : RequestBody.create(json, MediaType.parse(mediaType));
        return execute(authedRequest(acceptedPolicyDefinitionsUrl()).post(requestBody).build());
    }


    protected Response postAcceptedPolicyDefinition(String json) {
        return execute(authedRequest(acceptedPolicyDefinitionsUrl()).post(jsonBody(json)).build());
    }


    protected Response putAcceptedPolicyDefinition(String json, String mediaType) {
        var requestBody = mediaType == null ? RequestBody.create(new byte[0]) : RequestBody.create(json, MediaType.parse(mediaType));
        return execute(authedRequest(acceptedPolicyDefinitionsUrl()).put(requestBody).build());
    }


    protected Response putAcceptedPolicyDefinition(String json) {
        return execute(authedRequest(acceptedPolicyDefinitionsUrl()).put(jsonBody(json)).build());
    }


    protected Response deleteAcceptedPolicyDefinition(String policyDefinitionId) {
        return execute(authedRequest(acceptedPolicyDefinitionsUrl() + "/" + policyDefinitionId).delete().build());
    }


    private static RequestBody jsonBody(String json) {
        return RequestBody.create(json, JSON_MEDIA_TYPE);
    }


    /**
     * Waits until the runtime answers an (authenticated) request to the probe URL with any HTTP response. Booting
     * is performed synchronously by {@link RuntimePerClassExtension}, so this only guards against slow Jetty
     * initialization and CI jitter.
     */
    protected static void awaitRuntimeReadiness(String probeUrl) {
        var deadline = System.currentTimeMillis() + 60000;
        IOException lastFailure = null;
        while (System.currentTimeMillis() < deadline) {
            try (Response response = CLIENT.newCall(new Request.Builder().url(probeUrl).header(X_API_KEY, AUTH_KEY).get().build()).execute()) {
                return;
            }
            catch (IOException e) {
                lastFailure = e;
                try {
                    Thread.sleep(500);
                }
                catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    fail("Interrupted while awaiting runtime readiness for " + probeUrl);
                }
            }
        }
        fail("Runtime did not answer " + probeUrl + " in time: " + lastFailure);
    }
}
