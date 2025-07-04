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
package de.fraunhofer.iosb.app.aas.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.aas.lib.model.AasProvider;
import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import dev.failsafe.RetryPolicy;
import okhttp3.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.edc.http.client.EdcHttpClientImpl;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static de.fraunhofer.iosb.app.util.HttpClientProvider.clientFor;
import static de.fraunhofer.iosb.app.util.InetTools.getSelfSignedCertificate;
import static de.fraunhofer.iosb.app.util.InetTools.isConnectionTrusted;
import static jakarta.ws.rs.HttpMethod.GET;

/**
 * Fetching an AAS environment from AAS service or AAS registry providers.
 */
public abstract class AasAgent<T extends AasProvider, U> extends PipelineStep<T, U> {

    private final EdcHttpClient edcHttpClient;
    private final Monitor monitor;
    private final boolean allowSelfSigned;
    private final JsonDeserializer jsonDeserializer = new JsonDeserializer();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AasAgent(EdcHttpClient edcHttpClient, Monitor monitor, boolean allowSelfSigned) {
        this.edcHttpClient = edcHttpClient;
        this.monitor = monitor;
        this.allowSelfSigned = allowSelfSigned;
    }

    protected <K> Result<List<K>> readElements(AasProvider provider, String path, Class<K> clazz) {
        var responseResult = executeRequest(provider, path);

        if (responseResult.failed()) {
            // TODO get monitor in this class
            System.out.println(responseResult.getFailureMessages().toString());
            System.out.println(responseResult.getFailure().getMessages().toString());
            return Result.failure("Reading %s from %s failed: %s"
                    .formatted(clazz.getName(), path, responseResult.getFailureDetail()));
        }

        var response = responseResult.getContent();

        if (response.isSuccessful() && response.body() != null) {
            return readList(response.body(), clazz);
        }

        return Result.failure("Reading %s from %s failed: %s, %s"
                .formatted(clazz.getSimpleName(), path, response.code(), response.message()));
    }

    private Result<Response> executeRequest(AasProvider provider, String path) {
        var accessUrl = provider.getAccessUrl();

        var httpClient = getHttpClient(accessUrl);

        var requestUrlBuilder = HttpUrl.get(accessUrl.toString()).newBuilder();

        var requestPath = path;

        if (!requestPath.isEmpty()) {
            // Remove leading forward slash
            requestPath = requestPath.startsWith("/") ? requestPath.substring(1) : requestPath;
            requestUrlBuilder.addPathSegments(requestPath);
        }

        var requestUrl = requestUrlBuilder.build().url();

        var request = new Request.Builder()
                .method(GET, null)
                .url(requestUrl) // .url(HttpUrl) is marked as "internal"
                // Add potential authentication
                .headers(Headers.of(provider.getHeaders()))
                .build();
        try {
            return Result.success(httpClient.execute(request));
        } catch (IOException httpIOException) {
            return Result.failure(List.of(httpIOException.getClass().getSimpleName(), httpIOException.getMessage()));
        }
    }

    private EdcHttpClient getHttpClient(URL url) {
        if (isConnectionTrusted(url) || !allowSelfSigned) {
            return edcHttpClient;
        }
        var certificate = getSelfSignedCertificate(url);

        if (certificate.failed()) {
            // Still try to get data with default EDC HTTP client
            return edcHttpClient;
        }

        var okHttpClient = clientFor(certificate.getContent());

        if (okHttpClient.succeeded()) {
            return new EdcHttpClientImpl(okHttpClient.getContent(), RetryPolicy.ofDefaults(), monitor);
        }

        // Still try to get data with default EDC HTTP client
        return edcHttpClient;
    }

    private <K> @Nonnull Result<List<K>> readList(@Nullable ResponseBody responseBody, Class<K> clazz) {
        String serialized;
        try {
            serialized = responseBody == null ? null : responseBody.string();
        } catch (IOException readBodyException) {
            return Result.failure("Failed reading response body: %s, %s"
                    .formatted(readBodyException.getClass().getSimpleName(), readBodyException.getMessage()));
        }

        try {
            var responseJson = objectMapper.readTree(serialized).get("result");
            return Result.success(Optional.ofNullable(jsonDeserializer.readList(responseJson, clazz))
                    .orElse(new ArrayList<>()));
        } catch (JsonProcessingException | DeserializationException e) {
            return Result.failure(List.of("Failed parsing list of %s".formatted(clazz.getName()), e.getMessage(), ExceptionUtils.getRootCauseMessage(e)));
        }
    }
}
