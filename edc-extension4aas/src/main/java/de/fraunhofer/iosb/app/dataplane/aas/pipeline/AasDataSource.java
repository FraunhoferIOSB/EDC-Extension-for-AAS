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
package de.fraunhofer.iosb.app.dataplane.aas.pipeline;

import de.fraunhofer.iosb.app.util.HttpRestClient;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Request;
import org.eclipse.edc.connector.dataplane.http.pipeline.HttpPart;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress.OCTET_STREAM;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult.error;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult.success;

/**
 * Data source for new FA³ST with possibly self-signed certificate.
 * Inspired by {@link org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress}
 */
public class AasDataSource implements DataSource {

    private static final int FORBIDDEN = 401;
    private static final int NOT_AUTHORIZED = 403;
    private static final int NOT_FOUND = 404;

    private String requestId;
    private Monitor monitor;
    private HttpRestClient httpClient;
    private String baseUrl;
    private String path;
    private Map<String, String> headers;

    private AasDataSource() {
    }

    @Override
    public StreamResult<Stream<Part>> openPartStream() {
        var request = new Request.Builder()
                .url(baseUrl + path)
                .headers(Headers.of(headers))
                .get()
                .build();

        try (var response = httpClient.execute(request)) {
            if (response.isSuccessful()) {
                var body = response.body();
                if (body == null) {
                    throw new EdcException(format("Received empty response body transferring AAS data for request %s: %s", requestId, response.code()));
                }
                var bodyStream = body.byteStream();
                var mediaType = Optional.ofNullable(body.contentType()).map(MediaType::toString).orElse(OCTET_STREAM);
                body.close();
                return success(Stream.of(new HttpPart("AAS Part", bodyStream, mediaType)));

            } else {
                try {
                    if (NOT_AUTHORIZED == response.code() || FORBIDDEN == response.code()) {
                        return StreamResult.notAuthorized();
                    } else if (NOT_FOUND == response.code()) {
                        return StreamResult.notFound();
                    } else {
                        return error(format("Received code transferring AAS data: %s - %s.", response.code(), response.message()));
                    }
                } finally {
                    try {
                        response.close();
                    } catch (Exception e) {
                        monitor.info("Error closing failed response", e);
                    }
                }
            }

        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public void close() {
        // no-op

    }

    public static class Builder {
        private final AasDataSource dataSource;

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            dataSource = new AasDataSource();
        }

        public Builder requestId(String requestId) {
            dataSource.requestId = requestId;
            return this;
        }

        public Builder httpClient(HttpRestClient httpClient) {
            dataSource.httpClient = httpClient;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            dataSource.monitor = monitor;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            dataSource.baseUrl = baseUrl;
            return this;
        }

        public Builder path(String urlPath) {
            dataSource.path = urlPath;
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            dataSource.headers = headers;
            return this;
        }

        public AasDataSource build() {
            Objects.requireNonNull(dataSource.requestId, "requestId");
            Objects.requireNonNull(dataSource.httpClient, "httpClient");
            Objects.requireNonNull(dataSource.monitor, "monitor");
            Objects.requireNonNull(dataSource.baseUrl, "baseUrl");
            Objects.requireNonNull(dataSource.path, "path");
            return dataSource;
        }

    }

}
