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
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamFailure;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.AbstractResult;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult.failure;

/**
 * Retrieves the parts from dataSource and send them given the aas data processor.
 */
public class AasDataSink implements DataSink {

    private AasDataProcessorFactory aasDataProcessorFactory;
    private AasDataAddress aasDataAddress;
    private Monitor monitor;

    private AasDataSink() {

    }

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
        var aasDataProcessor = aasDataProcessorFactory.processorFor(aasDataAddress.getBaseUrl());

        if (aasDataProcessor.failed()) {
            monitor.severe("Error writing HTTP data %s to endpoint %s:\n%s".formatted(part.name(), aasDataAddress.getBaseUrl(),
                    aasDataProcessor.getFailureMessages()));

            return StreamResult.failure(new StreamFailure(aasDataProcessor.getFailureMessages(), StreamFailure.Reason.GENERAL_ERROR));
        }

        try (var response = aasDataProcessor.getContent().send(aasDataAddress, part)) {
            return StreamResult.success("DataTransfer completed. Response from consumer: " + response.body());
        } catch (IOException e) {
            var errorMessage = "IOException while data transferring to AAS: " + e.getMessage();
            monitor.severe(() -> errorMessage, e);
            return StreamResult.error(errorMessage);
        }

    }

    public static class Builder {
        private final AasDataSink dataSink;

        private Builder() {
            dataSink = new AasDataSink();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder aasDataAddress(AasDataAddress aasDataAddress) {
            dataSink.aasDataAddress = aasDataAddress;
            return this;
        }

        public Builder aasManipulator(AasDataProcessorFactory aasDataProcessor) {
            dataSink.aasDataProcessorFactory = aasDataProcessor;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            dataSink.monitor = monitor;
            return this;
        }

        public AasDataSink build() {
            Objects.requireNonNull(dataSink.aasDataProcessorFactory, "aasManipulator");
            Objects.requireNonNull(dataSink.aasDataAddress, "aasDataAddress");
            return dataSink;
        }
    }
}
