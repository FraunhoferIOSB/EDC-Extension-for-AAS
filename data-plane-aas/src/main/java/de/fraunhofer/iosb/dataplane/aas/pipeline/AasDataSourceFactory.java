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

import de.fraunhofer.iosb.aas.lib.AasDataProcessorFactory;
import de.fraunhofer.iosb.aas.lib.spi.AasDataAddress;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.jetbrains.annotations.NotNull;

import static de.fraunhofer.iosb.aas.lib.spi.AasDataAddress.AAS_DATA_TYPE;


/**
 * Inspired by org.eclipse.edc.connector.dataplane.http.pipeline.HttpDataSourceFactory
 */
public class AasDataSourceFactory implements DataSourceFactory {

    private final Monitor monitor;
    private final AasDataProcessorFactory aasDataProcessorFactory;

    public AasDataSourceFactory(Monitor monitor, AasDataProcessorFactory aasDataProcessorFactory) {
        this.monitor = monitor;
        this.aasDataProcessorFactory = aasDataProcessorFactory;
    }

    @Override
    public String supportedType() {
        return AAS_DATA_TYPE;
    }

    @Override
    public DataSource createSource(DataFlowStartMessage request) {
        var dataAddress = AasDataAddress.Builder.newInstance()
                .copyFrom(request.getSourceDataAddress());
        var destination = request.getDestinationDataAddress();
        if (isOperationRequest(destination)) {
            // TODO Currently injecting source DA with destination DA properties
            // https://faaast-service.readthedocs.io/en/latest/interfaces/endpoint.html#http
            dataAddress.method("POST")
                    .operation(AasDataAddress.Builder.newInstance().copyFrom(destination).build().getOperation());
        }

        return AasDataSource.Builder.newInstance()
                .aasDataProcessorFactory(aasDataProcessorFactory)
                .monitor(monitor)
                .requestId(request.getId())
                .aasDataAddress(dataAddress.build())
                .build();
    }

    private boolean isOperationRequest(DataAddress address) {
        return address.hasProperty(AasDataAddress.OPERATION_NAME);
    }

    @Override
    public @NotNull Result<Void> validateRequest(DataFlowStartMessage request) {
        try (var ignored = createSource(request)) {
            return Result.success();
        } catch (Exception e) {
            return Result.failure("Failed to validate AAS data source: " + e.getMessage());
        }
    }
}
