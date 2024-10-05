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
package de.fraunhofer.iosb.dataplane.aas.pipeline;

import de.fraunhofer.iosb.aas.AasDataProcessorFactory;
import de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamFailure;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * Data source for new FA³ST with possibly self-signed certificate.
 * Inspired by HttpDataAddress
 */
public class AasDataSource implements DataSource {

    private static final int FORBIDDEN = 401;
    private static final int NOT_AUTHORIZED = 403;
    private static final int NOT_FOUND = 404;
    private static final String APPLICATION_JSON = "application/json";
    private final AtomicReference<ResponseBodyStream> responseBodyStream = new AtomicReference<>();
    private String requestId;
    private Monitor monitor;
    private AasDataProcessorFactory aasDataProcessorFactory;
    private AasDataAddress aasDataAddress;

    private AasDataSource() {
    }

    @Override
    public StreamResult<Stream<Part>> openPartStream() {
        URL accessUrl;
        try {
            accessUrl = new URL(aasDataAddress.getBaseUrl());
        } catch (MalformedURLException e) {
            return StreamResult.failure(new StreamFailure(List.of(e.getMessage()), StreamFailure.Reason.GENERAL_ERROR));
        }

        var aasDataProcessor = aasDataProcessorFactory.processorFor(accessUrl);

        if (aasDataProcessor.failed()) {
            return StreamResult.failure(new StreamFailure(aasDataProcessor.getFailureMessages(), StreamFailure.Reason.GENERAL_ERROR));
        }

        try {
            // NB: Do not close the response as the body input stream needs to be read after this method returns. The response closes the body stream.
            var response = aasDataProcessor.getContent().send(aasDataAddress);

            if (response.isSuccessful()) {
                var body = response.body();
                if (body == null) {
                    throw new EdcException(format("Received empty response body transferring AAS data for request %s: %s", requestId, response.code()));
                }
                var bodyStream = body.byteStream();
                responseBodyStream.set(new ResponseBodyStream(body, bodyStream));
                var mediaType = Optional.ofNullable(body.contentType()).map(MediaType::toString).orElse(APPLICATION_JSON);
                return StreamResult.success(Stream.of(new AasPart("AAS Part", bodyStream, mediaType)));

            } else {
                try {
                    if (NOT_AUTHORIZED == response.code() || FORBIDDEN == response.code()) {
                        return StreamResult.notAuthorized();
                    } else if (NOT_FOUND == response.code()) {
                        return StreamResult.notFound();
                    } else {
                        return StreamResult.error(format("Received code transferring AAS data: %s - %s.", response.code(), response.message()));
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
                // do nothing --> closing the response body should close the inputStream as well...
            }
        }
    }

    private record ResponseBodyStream(ResponseBody responseBody, InputStream stream) {
    }

    public static class Builder {
        private final AasDataSource dataSource;

        private Builder() {
            dataSource = new AasDataSource();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder requestId(String requestId) {
            dataSource.requestId = requestId;
            return this;
        }

        public Builder aasDataProcessorFactory(AasDataProcessorFactory aasDataProcessor) {
            dataSource.aasDataProcessorFactory = aasDataProcessor;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            dataSource.monitor = monitor;
            return this;
        }

        public Builder aasDataAddress(AasDataAddress aasDataAddress) {
            dataSource.aasDataAddress = aasDataAddress;
            return this;
        }


        public AasDataSource build() {
            Objects.requireNonNull(dataSource.requestId, "requestId");
            Objects.requireNonNull(dataSource.aasDataProcessorFactory, "httpClient");
            Objects.requireNonNull(dataSource.monitor, "monitor");
            Objects.requireNonNull(dataSource.aasDataAddress, "aasDataAddress");

            return dataSource;
        }

    }

}
