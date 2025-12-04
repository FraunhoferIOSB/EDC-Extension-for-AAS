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
import de.fraunhofer.iosb.api.model.Endpoint;
import de.fraunhofer.iosb.api.model.HttpMethod;
import de.fraunhofer.iosb.app.controller.ConfigurationController;
import de.fraunhofer.iosb.app.controller.RegistryController;
import de.fraunhofer.iosb.app.controller.RepositoryController;
import de.fraunhofer.iosb.app.controller.SelfDescriptionController;
import de.fraunhofer.iosb.app.controller.dto.LocalRepositoryDTO;
import de.fraunhofer.iosb.app.controller.dto.RemoteAasRepositoryContextDTO;
import de.fraunhofer.iosb.app.edc.policy.PolicyHelper;
import de.fraunhofer.iosb.app.handler.edc.EdcStoreHandler;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.app.stores.repository.AasServerStore;
import de.fraunhofer.iosb.client.exception.UnauthorizedException;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.participantcontext.spi.identity.ParticipantIdentityResolver;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.Hostname;
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

    @Inject // Register public endpoints
    private PublicApiManagementService publicApiManagementService;
    @Inject
    private AssetIndex assetIndex;
    @Inject
    private ContractDefinitionStore contractDefinitionStore;
    @Inject
    private Hostname hostname;
    @Inject // context-specific participant-id
    private ParticipantIdentityResolver participantIdentityResolver;
    @Inject // Create / manage EDC policies
    private PolicyDefinitionStore policyDefinitionStore;
    @Inject // Register http endpoint at EDC
    private WebService webService;
    @Inject // Add AAS namespace to JSON LD context
    private JsonLd jsonLd;
    private RepositoryController repositoryController;
    private RegistryController registryController;
    private Monitor monitor;


    @Override
    public void initialize(ServiceExtensionContext context) {
        jsonLd.registerNamespace(AAS_PREFIX, AAS_V30_NAMESPACE);

        monitor = context.getMonitor().withPrefix(NAME);
        webService.registerResource(new ConfigurationController(context.getConfig(EDC_SETTINGS_PREFIX), monitor));

        AasServerStore aasServerStore = new AasServerStore();

        repositoryController = new RepositoryController(monitor, aasServerStore, hostname, new EdcStoreHandler(assetIndex, contractDefinitionStore));
        registryController = new RegistryController(monitor, aasServerStore, new EdcStoreHandler(assetIndex, contractDefinitionStore));

        // Add public endpoint if wanted by config
        if (Configuration.getInstance().isExposeSelfDescription()) {
            publicApiManagementService.addEndpoints(List.of(new Endpoint(SELF_DESCRIPTION_PATH, HttpMethod.GET,
                    Map.of())));
        }

        webService.registerResource(new SelfDescriptionController(monitor, aasServerStore));
        webService.registerResource(repositoryController);
        webService.registerResource(registryController);

        // This will probably fail if multiple participantIds are registered
        String participantId = participantIdentityResolver.getParticipantId("default", "DSP");

        PolicyHelper.registerDefaultPolicies(monitor, policyDefinitionStore, participantId);
        monitor.debug(String.format("%s initialized.", NAME));
    }


    @Override
    public void start() {
        try {
            bootstrapRepositories();
        }
        catch (UnauthorizedException e) {
            throw new EdcException("Unauthorized exception on registration of configured AAS repositories", e);
        }
        catch (ConnectException e) {
            throw new EdcException("Connect exception on registration of configured AAS repositories", e);
        }
        monitor.debug(String.format("%s started.", NAME));
    }


    private void bootstrapRepositories() throws UnauthorizedException, ConnectException {
        var configInstance = Configuration.getInstance();

        if (Objects.nonNull(configInstance.getRemoteAasLocation())) {
            var remoteRepositoryDto = new RemoteAasRepositoryContextDTO(configInstance.getRemoteAasLocation());
            URI serviceUri = repositoryController.register(remoteRepositoryDto);
            monitor.debug(String.format("Registered AAS repository with url %s", serviceUri));
        }

        if (Objects.isNull(configInstance.getLocalAasModelPath())) {
            return;
        }

        var localRepositoryDto = new LocalRepositoryDTO(
                configInstance.getLocalAasModelPath(),
                configInstance.getLocalAasServicePort(),
                configInstance.getAasServiceConfigPath());

        URI serviceUri = repositoryController.register(localRepositoryDto);
        monitor.debug(String.format("Started FA³ST service with url %s", serviceUri));
    }


    @Override
    public void shutdown() {
        repositoryController.unregisterAll();
        registryController.unregisterAll();
    }
}
