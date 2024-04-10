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
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository;
import de.fraunhofer.iosb.app.sync.Synchronizer;
import org.eclipse.edc.api.auth.spi.AuthenticationService;
import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
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
    private AuthenticationService authenticationService;
    @Inject
    private ContractDefinitionStore contractStore;
    @Inject
    private EdcHttpClient edcHttpClient;
    @Inject
    private PolicyDefinitionStore policyStore;
    @Inject
    private WebService webService;

    private static final String SETTINGS_PREFIX = "edc.aas";
    private static final Logger LOGGER = Logger.getInstance();
    private final ScheduledExecutorService syncExecutor = new ScheduledThreadPoolExecutor(1);
    private AasController aasController;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var configurationController = new ConfigurationController(context.getConfig(SETTINGS_PREFIX));

        // Distribute controllers, repository
        var selfDescriptionRepository = new SelfDescriptionRepository();
        this.aasController = new AasController(edcHttpClient);
        var endpoint = new Endpoint(selfDescriptionRepository, this.aasController, configurationController);

        // Initialize/Start synchronizer, start AAS services defined in configuration
        initializeSynchronizer(selfDescriptionRepository);
        registerServicesByConfig(selfDescriptionRepository);

        // Add public endpoint if wanted by config
        if (Configuration.getInstance().isExposeSelfDescription()) {
            publicApiManagementService.addEndpoints(List.of(new de.fraunhofer.iosb.api.model.Endpoint(Endpoint.SELF_DESCRIPTION_PATH, HttpMethod.GET, null)));
        }

        webService.registerResource(endpoint);
    }

    private void registerServicesByConfig(SelfDescriptionRepository selfDescriptionRepository) {
        var configInstance = Configuration.getInstance();

        if (Objects.nonNull(configInstance.getRemoteAasLocation())) {
            selfDescriptionRepository.createSelfDescription(configInstance.getRemoteAasLocation());
        }

        if (Objects.nonNull(configInstance.getLocalAasModelPath())) {
            try {
                Path aasConfigPath = null;
                if (Objects.nonNull(configInstance.getAasServiceConfigPath())) {
                    aasConfigPath = Path.of(configInstance.getAasServiceConfigPath());
                }
                var serviceUrl = aasController.startService(
                        Path.of(configInstance.getLocalAasModelPath()),
                        configInstance.getLocalAasServicePort(),
                        aasConfigPath);

                selfDescriptionRepository.createSelfDescription(serviceUrl);
            } catch (IOException startAssetAdministrationShellException) {
                LOGGER.warning("Could not start AAS service provided by configuration", startAssetAdministrationShellException);
            }
        }

    }

    private void initializeSynchronizer(SelfDescriptionRepository selfDescriptionRepository) {
        var synchronizer = new Synchronizer(selfDescriptionRepository, aasController,
                new ResourceController(assetIndex, contractStore, policyStore));
        selfDescriptionRepository.registerListener(synchronizer);

        // Task: get all AAS service URLs, synchronize EDC and AAS
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
