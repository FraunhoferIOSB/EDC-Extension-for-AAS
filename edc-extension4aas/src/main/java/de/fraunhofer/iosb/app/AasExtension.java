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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.utils.URIBuilder;
import org.eclipse.edc.api.auth.spi.AuthenticationService;
import org.eclipse.edc.connector.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.connector.spi.catalog.CatalogService;
import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.web.spi.WebService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.fraunhofer.iosb.app.client.ClientEndpoint;
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
    private AssetIndex assetIndex;
    @Inject
    private ConsumerContractNegotiationManager consumerNegotiationManager;
    @Inject
    private CatalogService catalogService;
    @Inject
    private ContractDefinitionStore contractStore;
    @Inject
    private OkHttpClient okHttpClient;
    @Inject
    private PolicyDefinitionStore policyStore;
    @Inject
    private TransferProcessManager transferProcessManager;
    @Inject
    private WebService webService;
    @Inject
    private ContractNegotiationObservable contractNegotiationObservable;
    @Inject
    private AuthenticationService authenticationService;

    private static final String SETTINGS_PREFIX = "edc.aas.";
    private final Logger logger = Logger.getInstance();
    private AasController aasController;
    private final ScheduledExecutorService syncExecutor = new ScheduledThreadPoolExecutor(1);

    @Override
    public void initialize(ServiceExtensionContext context) {
        logger.setMonitor(context.getMonitor());

        // Distribute controllers, repositories
        var selfDescriptionRepository = new ConcurrentHashMap<URL, SelfDescription>();
        aasController = new AasController(okHttpClient);

        var endpoint = new Endpoint(selfDescriptionRepository, aasController,
                new ResourceController(assetIndex, contractStore, policyStore));

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
        webService.registerResource(
                new ClientEndpoint(createOwnUriFromConfigurationValues(context.getConfig()),
                        catalogService,
                        consumerNegotiationManager, contractNegotiationObservable,
                        transferProcessManager));

        webService.registerResource(new CustomAuthenticationRequestFilter(authenticationService));
    }

    private URI createOwnUriFromConfigurationValues(Config config) {
        URL idsAddress;
        try {
            idsAddress = new URL(config.getString("ids.webhook.address"));
        } catch (MalformedURLException IdsWebhookAddressException) {
            throw new EdcException("Configuration value ids.webhook.address is a malformed URL",
                    IdsWebhookAddressException);
        }

        int ownPort = Integer.parseInt(config.getString("web.http.port"));
        String ownPath = config.getString("web.http.path");

        var ownUriBuilder = new URIBuilder();
        ownUriBuilder.setScheme(idsAddress.getProtocol())
                .setHost(idsAddress.getHost())
                .setPort(ownPort)
                .setPath(ownPath);

        try {
            return ownUriBuilder.build();
        } catch (URISyntaxException ownUriBuildException) {
            throw new EdcException("Own URI could not be built", ownUriBuildException);
        }
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
