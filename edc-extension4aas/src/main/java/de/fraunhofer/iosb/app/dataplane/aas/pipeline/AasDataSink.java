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
import org.eclipse.edc.connector.dataplane.http.params.HttpRequestFactory;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParams;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.AbstractResult;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult.failure;

/**
 * Retrieve the parts from dataSource and send them given
 * the HttpRequestParams and EdcExtension custom http client.
 * This works similarly to HttpDataSink but uses a custom
 * HttpClient accepting certain self-signed certificates.
 * TODO Since foreign AAS services should have valid certificates
 * TODO this sink will probably not be needed in the future
 * TODO (except if we want to send data to completely untrusted servers)
 */
public class AasDataSink implements DataSink {

    private HttpRequestFactory requestFactory;
    private HttpRestClient httpClient;
    private HttpRequestParams params;
    private Monitor monitor;

    @Override
    public CompletableFuture<StreamResult<Object>> transfer(DataSource dataSource) {
        var streamResult = dataSource.openPartStream();
        if (streamResult.failed()) {
            return completedFuture(failure(streamResult.getFailure()));
        }

        return streamResult.getContent()
                .map(this::transferPart)
                .allMatch(AbstractResult::succeeded) ?
                completedFuture(StreamResult.success()) :
                completedFuture(StreamResult.error("Some parts were not transferred correctly"));
    }

    private StreamResult<Object> transferPart(DataSource.Part part) {
        var request = requestFactory.toRequest(params, part);

        monitor.debug(() -> "Executing HTTP request to AAS service: " + request.url());

        try (var response = httpClient.execute(request)) {
            return StreamResult.success("DataTransfer completed. Response from consumer: " + response.body());
        } catch (IOException e) {
            var errorMessage = "IOException while data transferring: " + e.getMessage();
            monitor.severe(() -> errorMessage);
            return StreamResult.error(errorMessage);
        }

    }

    public static class Builder {
        private final AasDataSink dataSink;

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            dataSink = new AasDataSink();
        }

        public Builder params(HttpRequestParams params) {
            dataSink.params = params;
            return this;
        }

        public Builder httpClient(HttpRestClient httpClient) {
            dataSink.httpClient = httpClient;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            dataSink.monitor = monitor;
            return this;
        }

        public Builder requestFactory(HttpRequestFactory requestFactory) {
            dataSink.requestFactory = requestFactory;
            return this;
        }

        public AasDataSink build() {
            Objects.requireNonNull(dataSink.httpClient, "httpClient");
            Objects.requireNonNull(dataSink.requestFactory, "requestFactory");
            return dataSink;
        }
    }
}
