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

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.edc.api.auth.spi.AuthenticationService;
import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.fraunhofer.iosb.app.authentication.CustomAuthenticationRequestFilter;
import de.fraunhofer.iosb.app.controller.AasController;
import de.fraunhofer.iosb.app.controller.ConfigurationController;
import de.fraunhofer.iosb.app.controller.ResourceController;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository;
import de.fraunhofer.iosb.app.sync.Synchronizer;
import okhttp3.OkHttpClient;

/**
 * EDC Extension supporting usage of Asset Administration Shells.
 */
public class AasExtension implements ServiceExtension {

    @Inject
    private AssetIndex assetIndex;
    @Inject
    private AuthenticationService authenticationService;
    @Inject
    private ContractDefinitionStore contractStore;
    @Inject
    private OkHttpClient okHttpClient;
    @Inject
    private PolicyDefinitionStore policyStore;
    @Inject
    private WebService webService;

    private static final String SETTINGS_PREFIX = "edc.aas.";
    private static final Logger logger = Logger.getInstance();
    private final ScheduledExecutorService syncExecutor = new ScheduledThreadPoolExecutor(1);
    private AasController aasController;

    @Override
    public void initialize(ServiceExtensionContext context) {
        logger.setMonitor(context.getMonitor());

        // Distribute controllers, repositories
        var selfDescriptionRepository = new SelfDescriptionRepository();
        aasController = new AasController(okHttpClient);
        var endpoint = new Endpoint(selfDescriptionRepository, aasController);
        var synchronizer = new Synchronizer(selfDescriptionRepository, aasController,
                new ResourceController(assetIndex, contractStore, policyStore));
        selfDescriptionRepository.registerListener(synchronizer);

        loadConfig(context);
        var configInstance = Configuration.getInstance();

        // Remote AAS service URL supplied?
        if (Objects.nonNull(configInstance.getRemoteAasLocation())) {
            endpoint.postAasService(configInstance.getRemoteAasLocation());
        }

        // AAS model supplied?
        if (Objects.nonNull(configInstance.getLocalAasModelPath())) {
            endpoint.postAasEnvironment(configInstance.getLocalAasModelPath(), configInstance.getAasServiceConfigPath(),
                    configInstance.getLocalAasServicePort());
        }

        // Task: get all AAS service URLs, synchronize EDC and AAS
        syncExecutor.scheduleAtFixedRate(
                synchronizer::synchronize,
                1,
                configInstance.getSyncPeriod(), TimeUnit.SECONDS);

        webService.registerResource(endpoint);

        var authenticationRequestFilter = new CustomAuthenticationRequestFilter(authenticationService,
                configInstance.isExposeSelfDescription() ? Endpoint.SELF_DESCRIPTION_PATH : null);
        webService.registerResource(authenticationRequestFilter);

    }

    /**
     * Get extension specific configuration from EDC config object
     *
     * @param context EDC config reference provider
     */
    private void loadConfig(ServiceExtensionContext context) {
        var objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        var config = context.getConfig();

        String configAsString;
        try {
            configAsString = objectMapper.writeValueAsString(config.getRelativeEntries(SETTINGS_PREFIX));
        } catch (JsonProcessingException e) {
            // This should not be reached, unless there is an error inside EDCs Config.java
            logger.error("Could not load AAS extension configuration, using default values", e);
            configAsString = "";
        }
        new ConfigurationController().handleRequest(RequestType.PUT, null, configAsString);
    }

    @Override
    public void shutdown() {
        logger.log("Shutting down EDC4AAS extension...");
        syncExecutor.shutdown();
        aasController.stopServices();
    }
}
