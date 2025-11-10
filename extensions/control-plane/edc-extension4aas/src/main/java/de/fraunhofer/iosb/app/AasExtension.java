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

import de.fraunhofer.iosb.AasRepositoryRegistry;
import de.fraunhofer.iosb.aas.lib.auth.impl.NoAuth;
import de.fraunhofer.iosb.api.PublicApiManagementService;
import de.fraunhofer.iosb.api.model.Endpoint;
import de.fraunhofer.iosb.api.model.HttpMethod;
import de.fraunhofer.iosb.app.controller.AasRepositoryController;
import de.fraunhofer.iosb.app.controller.ConfigurationController;
import de.fraunhofer.iosb.app.controller.SelfDescriptionController;
import de.fraunhofer.iosb.app.controller.dto.LocalRepositoryDTO;
import de.fraunhofer.iosb.app.controller.dto.RemoteAasRepositoryContextDTO;

import de.fraunhofer.iosb.app.edc.contract.PolicyHelper;
import de.fraunhofer.iosb.app.handler.edc.EdcStoreHandler;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.app.stores.repository.AasServerStore;
import de.fraunhofer.iosb.client.exception.UnauthorizedException;
import de.fraunhofer.iosb.repository.impl.faaast.FaaastRepositoryManager;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;

import java.net.ConnectException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static de.fraunhofer.iosb.app.controller.SelfDescriptionController.SELF_DESCRIPTION_PATH;
import static de.fraunhofer.iosb.constants.AasConstants.AAS_PREFIX;
import static de.fraunhofer.iosb.constants.AasConstants.AAS_V30_NAMESPACE;
import static de.fraunhofer.iosb.constants.AasConstants.EDC_SETTINGS_PREFIX;

/**
 * EDC Extension supporting usage of Asset Administration Shells.
 */
@Extension(value = AasExtension.NAME)
public class AasExtension implements ServiceExtension {

    public static final String NAME = "EDC4AAS Extension";

    @Inject // Management of AAS repositories
    private AasRepositoryRegistry aasRepositoryRegistry;
    @Inject // Register public endpoints
    private PublicApiManagementService publicApiManagementService;
    @Inject
    private AssetIndex assetIndex;
    @Inject
    private ContractDefinitionStore contractDefinitionStore;
    @Inject // Create / manage EDC policies
    private PolicyDefinitionStore policyDefinitionStore;
    @Inject // Register http endpoint at EDC
    private WebService webService;
    @Inject // Add AAS namespace to JSON LD context
    private JsonLd jsonLd;
    private AasRepositoryController aasServerController;
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        jsonLd.registerNamespace(AAS_PREFIX, AAS_V30_NAMESPACE);

        monitor = context.getMonitor().withPrefix(NAME);
        webService.registerResource(new ConfigurationController(context.getConfig(EDC_SETTINGS_PREFIX), monitor));

        // Use FA³ST to start repositories internally.
        var faaastRepositoryManager = aasRepositoryRegistry.getFor(FaaastRepositoryManager.class);

        AasServerStore aasServerStore = new AasServerStore();

        aasServerController = new AasRepositoryController(monitor, aasServerStore, faaastRepositoryManager,
                new EdcStoreHandler(assetIndex, contractDefinitionStore));

        // Add public endpoint if wanted by config
        if (Configuration.getInstance().isExposeSelfDescription()) {
            publicApiManagementService.addEndpoints(List.of(new Endpoint(SELF_DESCRIPTION_PATH, HttpMethod.GET,
                    Map.of())));
        }

        webService.registerResource(new SelfDescriptionController(monitor, aasServerStore));
        webService.registerResource(aasServerController);

        String participantId = context.getParticipantId();

        PolicyHelper.registerDefaultPolicies(monitor, policyDefinitionStore, participantId);
        monitor.debug(String.format("%s initialized.", NAME));
    }

    @Override
    public void start() {
        try {
            registerAasServicesByConfig();
        } catch (UnauthorizedException e) {
            throw new EdcException("Unauthorized exception on registration of configured AAS servers", e);
        } catch (ConnectException e) {
            throw new EdcException("Connect exception on registration of configured AAS servers", e);
        }
        monitor.debug(String.format("%s started.", NAME));
    }

    private void registerAasServicesByConfig() throws UnauthorizedException, ConnectException {
        var configInstance = Configuration.getInstance();

        if (Objects.nonNull(configInstance.getRemoteAasLocation())) {
            var remoteRepositoryDto = new RemoteAasRepositoryContextDTO(
                    configInstance.getRemoteAasLocation(),
                    new NoAuth(),
                    null);
            URI serviceUri = aasServerController.register(remoteRepositoryDto);
            monitor.debug(String.format("Registered AAS repository with uri %s", serviceUri));
        }

        if (Objects.isNull(configInstance.getLocalAasModelPath())) {
            return;
        }

        var localRepositoryDto = new LocalRepositoryDTO(
                configInstance.getLocalAasModelPath(),
                configInstance.getLocalAasServicePort(),
                configInstance.getAasServiceConfigPath(),
                null);

        URI serviceUri = aasServerController.start(localRepositoryDto);
        monitor.debug(String.format("Started FA³ST service with uri %s", serviceUri));
    }

    @Override
    public void shutdown() {
        aasServerController.unregisterAll();
    }
}

