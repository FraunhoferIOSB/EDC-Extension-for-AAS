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

import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
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

import de.fraunhofer.iosb.app.controller.AasController;
import de.fraunhofer.iosb.app.controller.ConfigurationController;
import de.fraunhofer.iosb.app.controller.ResourceController;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.app.model.ids.SelfDescription;
import okhttp3.OkHttpClient;

/**
 * Extension providing/connecting EDC logic to the EDC-AAS-Application.
 */
public class AasExtension implements ServiceExtension {

    @Inject
    private WebService webService;
    @Inject
    private ContractDefinitionStore contractStore;
    @Inject
    private AssetIndex assetLoader;
    @Inject
    private PolicyDefinitionStore policyStore;
    @Inject
    private OkHttpClient okHttpClient;
    @Inject
    private AuthenticationService authenticationService;

    private static final String SETTINGS_PREFIX = "edc.aas.";
    private final Logger logger = Logger.getInstance();

    private Endpoint endpoint;
    private AasController aasController;

    private final ScheduledThreadPoolExecutor syncExecutor = new ScheduledThreadPoolExecutor(1);

    @Override
    public void initialize(ServiceExtensionContext context) {
        logger.setMonitor(context.getMonitor());

        // Distribute controllers, repositories
        var selfDescriptionRepository = new ConcurrentHashMap<URL, SelfDescription>();
        var resourceController = new ResourceController(assetLoader, contractStore, policyStore);
        aasController = new AasController(okHttpClient);

        endpoint = new Endpoint(selfDescriptionRepository, aasController, resourceController);

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
                () -> selfDescriptionRepository.keys().asIterator()
                        .forEachRemaining(url -> endpoint.syncAasWithEdc(url)),
                configInstance.getSyncPeriod(),
                configInstance.getSyncPeriod(), TimeUnit.SECONDS);

        webService.registerResource(endpoint);
        webService.registerResource(new CustomAuthenticationRequestFilter(authenticationService));
    }

    /**
     * Get extension specific configuration from EDC config object

     * @param context EDC config reference provider
     */
    private void loadConfig(ServiceExtensionContext context) {
        var objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        var config = context.getConfig();

        logger.setPrefix(config.getString(SETTINGS_PREFIX + "logPrefix", "AAS Extension"));

        String configAsString;
        try {
            configAsString = objectMapper.writeValueAsString(config.getRelativeEntries(SETTINGS_PREFIX));
        } catch (JsonProcessingException e) {
            // This should not be reached, unless there is an error inside EDC's Config.java
            logger.error("Could not load AAS extension configuration, using default values", e);
            configAsString = "";
        }
        new ConfigurationController().handleRequest(RequestType.PUT, null, configAsString);
    }

    @Override
    public void shutdown() {
        syncExecutor.shutdown();
        aasController.stopServices();
    }
}
