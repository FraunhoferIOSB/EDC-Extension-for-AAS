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
package de.fraunhofer.iosb.app;

import de.fraunhofer.iosb.api.PublicApiManagementService;
import de.fraunhofer.iosb.api.model.HttpMethod;
import de.fraunhofer.iosb.app.controller.AasController;
import de.fraunhofer.iosb.app.controller.ConfigurationController;
import de.fraunhofer.iosb.app.controller.ResourceController;
import de.fraunhofer.iosb.app.dataplane.aas.pipeline.AasDataSinkFactory;
import de.fraunhofer.iosb.app.dataplane.aas.pipeline.AasDataSourceFactory;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository;
import de.fraunhofer.iosb.app.sync.Synchronizer;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.connector.dataplane.http.params.HttpRequestParamsProviderImpl;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.WebService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * EDC Extension supporting usage of Asset Administration Shells.
 */
public class AasExtension implements ServiceExtension {

    // Non-public unified authentication request filter management service
    @Inject
    private PublicApiManagementService publicApiManagementService;

    @Inject
    private AssetIndex assetIndex;
    @Inject
    private ContractDefinitionStore contractStore;
    @Inject // AAS Data Source Factory
    private PipelineService pipelineService;
    @Inject
    private PolicyDefinitionStore policyStore;
    @Inject // AAS Data Source Factory
    private TypeManager typeManager;
    @Inject // AAS Data Source Factory
    private Vault vault;
    @Inject
    private WebService webService;

    private static final String SETTINGS_PREFIX = "edc.aas";
    private static final Logger LOGGER = Logger.getInstance();

    private ScheduledExecutorService syncExecutor;
    private AasController aasController;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var configurationController = new ConfigurationController(context.getConfig(SETTINGS_PREFIX));

        // Distribute controllers, repository
        var selfDescriptionRepository = new SelfDescriptionRepository();
        this.aasController = new AasController();
        var endpoint = new Endpoint(selfDescriptionRepository, this.aasController, configurationController);

        // Initialize/Start synchronizer, start AAS services defined in configuration
        initializeSynchronizer(selfDescriptionRepository);
        // This makes the connector shutdown if an exception occurs while starting config services
        registerServicesByConfig(selfDescriptionRepository);

        // Add public endpoint if wanted by config
        if (Configuration.getInstance().isExposeSelfDescription()) {
            publicApiManagementService.addEndpoints(List.of(new de.fraunhofer.iosb.api.model.Endpoint(Endpoint.SELF_DESCRIPTION_PATH, HttpMethod.GET, Map.of())));
        }

        // Register AAS Data Source factory for dataTransfer with self-signed certificates on FA³ST side
        var paramsProvider = new HttpRequestParamsProviderImpl(vault, typeManager);
        var aasDataSourceFactory = new AasDataSourceFactory(paramsProvider, context.getMonitor());
        var aasDataSinkFactory = new AasDataSinkFactory(paramsProvider, context.getMonitor());

        pipelineService.registerFactory(aasDataSourceFactory);
        pipelineService.registerFactory(aasDataSinkFactory);

        webService.registerResource(endpoint);
    }

    private void registerServicesByConfig(SelfDescriptionRepository selfDescriptionRepository) {
        var configInstance = Configuration.getInstance();
        Path aasConfigPath = null;

        if (Objects.nonNull(configInstance.getRemoteAasLocation())) {
            selfDescriptionRepository.createSelfDescription(configInstance.getRemoteAasLocation());
        }

        if (Objects.isNull(configInstance.getLocalAasModelPath())) {
            return;
        }

        if (Objects.nonNull(configInstance.getAasServiceConfigPath())) {
            aasConfigPath = Path.of(configInstance.getAasServiceConfigPath());
        }

        try {
            var serviceUrl = aasController.startService(
                    Path.of(configInstance.getLocalAasModelPath()),
                    configInstance.getLocalAasServicePort(),
                    aasConfigPath);

            selfDescriptionRepository.createSelfDescription(serviceUrl);
        } catch (IOException startAssetAdministrationShellException) {
            LOGGER.warning("Could not start AAS service provided by configuration", startAssetAdministrationShellException);
        }
    }

    private void initializeSynchronizer(SelfDescriptionRepository selfDescriptionRepository) {
        var synchronizer = new Synchronizer(selfDescriptionRepository, aasController,
                new ResourceController(assetIndex, contractStore, policyStore));
        selfDescriptionRepository.registerListener(synchronizer);

        // Task: get all AAS service URLs, synchronize EDC and AAS
        syncExecutor = new ScheduledThreadPoolExecutor(1);
        syncExecutor.scheduleAtFixedRate(synchronizer::synchronize, 1,
                Configuration.getInstance().getSyncPeriod(), TimeUnit.SECONDS);
    }

    @Override
    public void shutdown() {
        LOGGER.info("Shutting down EDC4AAS extension...");
        syncExecutor.shutdown();
        aasController.stopServices();
    }
}
