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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.app.authentication.CustomAuthenticationRequestFilter;
import de.fraunhofer.iosb.app.client.ClientEndpoint;
import de.fraunhofer.iosb.app.client.contract.PolicyService;
import de.fraunhofer.iosb.app.client.dataTransfer.DataTransferEndpoint;
import de.fraunhofer.iosb.app.client.dataTransfer.DataTransferObservable;
import de.fraunhofer.iosb.app.client.dataTransfer.TransferInitiator;
import de.fraunhofer.iosb.app.client.negotiation.Negotiator;
import de.fraunhofer.iosb.app.controller.AasController;
import de.fraunhofer.iosb.app.controller.ConfigurationController;
import de.fraunhofer.iosb.app.controller.ResourceController;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository;
import de.fraunhofer.iosb.app.sync.Synchronizer;
import okhttp3.OkHttpClient;
import org.apache.http.client.utils.URIBuilder;
import org.eclipse.edc.api.auth.spi.AuthenticationService;
import org.eclipse.edc.connector.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
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
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.WebService;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

/**
 * EDC Extension supporting usage of Asset Administration Shells.
 */
public class AasExtension implements ServiceExtension {

    @Inject
    private AssetIndex assetIndex;
    @Inject
    private AuthenticationService authenticationService;
    @Inject
    private CatalogService catalogService;
    @Inject
    private TypeTransformerRegistry transformer;
    @Inject
    private ConsumerContractNegotiationManager consumerNegotiationManager;
    @Inject
    private ContractDefinitionStore contractStore;
    @Inject
    private ContractNegotiationObservable contractNegotiationObservable;
    @Inject
    private ContractNegotiationStore contractNegotiationStore;
    @Inject
    private OkHttpClient okHttpClient;
    @Inject
    private PolicyDefinitionStore policyStore;
    @Inject
    private TransferProcessManager transferProcessManager;
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

        initializeClient(context, authenticationRequestFilter);
    }

    private void initializeClient(ServiceExtensionContext context,
                                  CustomAuthenticationRequestFilter authenticationRequestFilter) {
        URI ownUri;
        try {
            ownUri = createOwnUriFromConfigurationValues(context.getConfig());
        } catch (EdcException buildUriException) {
            logger.error("Own URI for client could not be built. Reason:", buildUriException);
            logger.warn("Client Endpoint will not be exposed and its functionality will not be available");
            return;
        }

        var observable = new DataTransferObservable();

        var clientEndpoint = new ClientEndpoint(new PolicyService(catalogService, transformer),
                new Negotiator(consumerNegotiationManager, contractNegotiationObservable,
                        contractNegotiationStore),
                new TransferInitiator(ownUri, transferProcessManager, observable,
                        authenticationRequestFilter));
        webService.registerResource(clientEndpoint);

        var dataTransferEndpoint = new DataTransferEndpoint(observable);
        webService.registerResource(dataTransferEndpoint);
    }

    /*
    Maybe there is another way to retrieve these values?
     */
    private URI createOwnUriFromConfigurationValues(Config config) {
        URL protocolAddress;
        var protocolAddressString = config.getString("edc.dsp.callback.address");

        try {
            protocolAddress = new URL(protocolAddressString);
        } catch (MalformedURLException idsWebhookAddressException) {
            throw new EdcException(format("Configuration value edc.dsp.callback.address is a malformed URL: %s",
                    protocolAddressString),
                    idsWebhookAddressException);
        }

        int ownPort = Integer.parseInt(config.getString("web.http.port"));
        String ownPath = config.getString("web.http.path");

        var ownUriBuilder = new URIBuilder()
                .setScheme(protocolAddress.getProtocol())
                .setHost(protocolAddress.getHost())
                .setPort(ownPort)
                .setPath(ownPath);

        try {
            return ownUriBuilder.build();
        } catch (URISyntaxException ownUriBuildException) {
            throw new EdcException("Own URI could not be built:", ownUriBuildException);
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
