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
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.eclipse.edc.connector.dataplane.http.params.HttpRequestFactory;
import org.eclipse.edc.connector.dataplane.http.pipeline.HttpPart;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParams;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
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

    private String name;
    private HttpRequestParams params;
    private String requestId;
    private Monitor monitor;
    private HttpRestClient httpClient;
    private HttpRequestFactory requestFactory;
    private final AtomicReference<ResponseBodyStream> responseBodyStream = new AtomicReference<>();

    private AasDataSource() {
    }

    @Override
    public StreamResult<Stream<Part>> openPartStream() {
        var request = requestFactory.toRequest(params);
        monitor.debug(() -> "Executing HTTP request to FA³ST: " + request.url());
        try {
            // NB: Do not close the response as the body input stream needs to be read after this method returns. The response closes the body stream.
            var response = httpClient.execute(request);
            if (response.isSuccessful()) {
                var body = response.body();
                if (body == null) {
                    throw new EdcException(format("Received empty response body transferring HTTP data for request %s: %s", requestId, response.code()));
                }
                var stream = body.byteStream();
                responseBodyStream.set(new ResponseBodyStream(body, stream));
                var mediaType = Optional.ofNullable(body.contentType()).map(MediaType::toString).orElse(OCTET_STREAM);
                return success(Stream.of(new HttpPart(name, stream, mediaType)));
            } else {
                try {
                    if (NOT_AUTHORIZED == response.code() || FORBIDDEN == response.code()) {
                        return StreamResult.notAuthorized();
                    } else if (NOT_FOUND == response.code()) {
                        return StreamResult.notFound();
                    } else {
                        return error(format("Received code transferring HTTP data: %s - %s.", response.code(), response.message()));
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
        var bodyStream = responseBodyStream.get();
        if (bodyStream != null) {
            bodyStream.responseBody().close();
            try {
                bodyStream.stream().close();
            } catch (IOException e) {
                // do nothing
            }
        }
    }

    private record ResponseBodyStream(ResponseBody responseBody, InputStream stream) {

    }

    public static class Builder {
        private final AasDataSource dataSource;

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            dataSource = new AasDataSource();
        }

        public Builder params(HttpRequestParams params) {
            dataSource.params = params;
            return this;
        }

        public Builder name(String name) {
            dataSource.name = name;
            return this;
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

        public Builder requestFactory(HttpRequestFactory requestFactory) {
            dataSource.requestFactory = requestFactory;
            return this;
        }

        public AasDataSource build() {
            Objects.requireNonNull(dataSource.requestId, "requestId");
            Objects.requireNonNull(dataSource.httpClient, "httpClient");
            Objects.requireNonNull(dataSource.monitor, "monitor");
            Objects.requireNonNull(dataSource.requestFactory, "requestFactory");
            return dataSource;
        }
    }

}
