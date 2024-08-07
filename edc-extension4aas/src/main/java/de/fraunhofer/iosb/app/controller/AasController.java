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
package de.fraunhofer.iosb.app.controller;

import de.fraunhofer.iosb.app.aas.AssetAdministrationShellServiceManager;
import de.fraunhofer.iosb.app.aas.FaaastServiceManager;
import de.fraunhofer.iosb.app.edc.asset.AssetRegistrar;
import de.fraunhofer.iosb.app.edc.contract.ContractRegistrar;
import de.fraunhofer.iosb.app.model.ChangeSet;
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionChangeListener;
import de.fraunhofer.iosb.app.pipeline.Pipeline;
import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import de.fraunhofer.iosb.app.util.AssetUtil;
import de.fraunhofer.iosb.registry.AasServiceRegistry;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;

import static de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository.SelfDescriptionMetaInformation;
import static java.lang.String.format;

/**
 * Handles requests regarding the Asset Administration Shells registered to this
 * extension
 */
public class AasController implements SelfDescriptionChangeListener {

    private final AssetAdministrationShellServiceManager aasServiceManager;
    private final Monitor monitor;
    private final AasServiceRegistry serviceRegistry;
    private final Pipeline<Asset, Void> cleanUpPipeline;

    public AasController(AasServiceRegistry aasServiceRegistry,
                         AssetIndex assetIndex,
                         ContractDefinitionStore contractDefinitionStore,
                         Monitor monitor,
                         PolicyDefinitionStore policyDefinitionStore) {
        this.monitor = monitor;
        serviceRegistry = aasServiceRegistry;

        aasServiceManager = new FaaastServiceManager(monitor);

        this.cleanUpPipeline = new Pipeline.Builder<Asset, ChangeSet<Asset, String>>()
                .initialStep(PipelineStep.create(asset -> new ChangeSet.Builder<Asset, String>()
                        .remove(AssetUtil
                                .flatMapAssets(asset).stream()
                                .map(Asset::getId)
                                .toList())
                        .build()))
                .step(new AssetRegistrar(assetIndex, monitor))
                .step(new ContractRegistrar(contractDefinitionStore, policyDefinitionStore, monitor))
                .monitor(monitor)
                .build();
    }

    /**
     * Starts an AAS service internally
     *
     * @param aasModelPath   AAS Environment for the AAS service
     * @param aasServicePort AAS service's exposed HTTP port for communication
     *                       with this extension
     * @param aasConfigPath  AAS config (optional)
     * @return The URL of the new service or null on error
     * @throws IOException If the URL creation fails
     */
    public URL startService(Path aasModelPath, int aasServicePort, Path aasConfigPath) throws IOException {
        Objects.requireNonNull(aasModelPath);

        URL serviceUrl;
        if (Objects.isNull(aasConfigPath)) {
            monitor.info(format(
                    "Booting up AAS service given AAS model path (%s)\n and service port (%s)\n...",
                    aasModelPath, aasServicePort));
            serviceUrl = aasServiceManager.startService(aasModelPath, aasServicePort);
        } else {
            monitor.info(format(
                    "Booting up AAS service given AAS model path (%s)\n and service config path (%s)...",
                    aasModelPath, aasConfigPath));
            serviceUrl = aasServiceManager.startService(aasModelPath, aasConfigPath, aasServicePort);
        }

        return serviceUrl;
    }

    /**
     * Stops an AAS service given its URL if internally started
     *
     * @param aasServiceUrl URL of service to be stopped
     */
    public void stopService(URL aasServiceUrl) {
        monitor.info(format("Shutting down AAS service with URL %s...", aasServiceUrl.toString()));
        aasServiceManager.stopService(aasServiceUrl);
    }

    /**
     * Stops all internally started AAS services
     */
    public void stopServices() {
        monitor.info("Shutting down all AAS services...");
        aasServiceManager.stopServices();
    }

    @Override
    public void created(SelfDescriptionMetaInformation metaInformation) {
        serviceRegistry.register(metaInformation.url().toString());
    }

    @Override
    public void removed(SelfDescriptionMetaInformation metaInformation, Asset asset) {
        serviceRegistry.unregister(metaInformation.url().toString());
        // Remove all assets, contracts
        cleanUpPipeline.execute(asset);
    }
}
