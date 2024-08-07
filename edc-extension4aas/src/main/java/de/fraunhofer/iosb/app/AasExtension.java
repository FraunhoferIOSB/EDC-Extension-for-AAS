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

import de.fraunhofer.iosb.aas.AasDataProcessorFactory;
import de.fraunhofer.iosb.api.PublicApiManagementService;
import de.fraunhofer.iosb.api.model.HttpMethod;
import de.fraunhofer.iosb.app.aas.EnvironmentToAssetMapper;
import de.fraunhofer.iosb.app.aas.agent.AasAgentSelector;
import de.fraunhofer.iosb.app.aas.agent.impl.RegistryAgent;
import de.fraunhofer.iosb.app.aas.agent.impl.ServiceAgent;
import de.fraunhofer.iosb.app.controller.AasController;
import de.fraunhofer.iosb.app.controller.ConfigurationController;
import de.fraunhofer.iosb.app.controller.SelfDescriptionController;
import de.fraunhofer.iosb.app.edc.CleanUpService;
import de.fraunhofer.iosb.app.edc.asset.AssetRegistrar;
import de.fraunhofer.iosb.app.edc.contract.ContractRegistrar;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository;
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionUpdater;
import de.fraunhofer.iosb.app.pipeline.Pipeline;
import de.fraunhofer.iosb.app.sync.Synchronizer;
import de.fraunhofer.iosb.app.util.VariableRateScheduler;
import de.fraunhofer.iosb.registry.AasServiceRegistry;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static de.fraunhofer.iosb.app.controller.SelfDescriptionController.SELF_DESCRIPTION_PATH;

/**
 * EDC Extension supporting usage of Asset Administration Shells.
 */
@Extension(value = AasExtension.NAME)
public class AasExtension implements ServiceExtension {

    public static final String NAME = "EDC4AAS Extension";

    private static final String SETTINGS_PREFIX = "edc.aas";
    @Inject
    private AasDataProcessorFactory aasDataProcessorFactory;
    @Inject // Register AAS services (with self-signed certs) to allow communication
    private AasServiceRegistry aasServiceRegistry;
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
    private AasController aasController;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor().withPrefix(NAME);
        webService.registerResource(new ConfigurationController(context.getConfig(SETTINGS_PREFIX), monitor));

        aasController = new AasController(aasServiceRegistry, monitor);
        var selfDescriptionRepository = new SelfDescriptionRepository();

        selfDescriptionRepository.registerListener(
                CleanUpService.Builder.newInstance()
                        .assetIndex(assetIndex)
                        .policyDefinitionStore(policyDefinitionStore)
                        .monitor(monitor)
                        .contractDefinitionStore(contractDefinitionStore)
                        .build()
        );

        selfDescriptionRepository.registerListener(aasController);
        registerAasServicesByConfig(selfDescriptionRepository);

        var pipeline = createSynchronizerPipeline(selfDescriptionRepository, monitor);
        new VariableRateScheduler(1).scheduleAtVariableRate(pipeline,
                () -> Configuration.getInstance().getSyncPeriod());

        // Add public endpoint if wanted by config
        if (Configuration.getInstance().isExposeSelfDescription()) {
            publicApiManagementService.addEndpoints(List.of(new de.fraunhofer.iosb.api.model.Endpoint(SELF_DESCRIPTION_PATH, HttpMethod.GET, Map.of())));
        }

        webService.registerResource(new SelfDescriptionController(monitor, selfDescriptionRepository));
        webService.registerResource(new Endpoint(selfDescriptionRepository, aasController, monitor));
    }


    private Pipeline<Void, Void> createSynchronizerPipeline(SelfDescriptionRepository repo, Monitor monitor) {
        return new Pipeline.Builder<Void, Void>()
                .monitor(monitor)
                .supplier(repo::getAllSelfDescriptionMetaInformation)
                .step(new AasAgentSelector(Set.of(new ServiceAgent(aasDataProcessorFactory),
                        new RegistryAgent(aasDataProcessorFactory))))
                .step(new EnvironmentToAssetMapper(() -> Configuration.getInstance().isOnlySubmodels()))
                .step(new SelfDescriptionUpdater(repo))
                .step(new Synchronizer())
                .step(new AssetRegistrar(assetIndex, monitor))
                .step(new ContractRegistrar(contractDefinitionStore, policyDefinitionStore, monitor))
                .build();

    }

    private void registerAasServicesByConfig(SelfDescriptionRepository selfDescriptionRepository) {
        var configInstance = Configuration.getInstance();

        if (Objects.nonNull(configInstance.getRemoteAasLocation())) {
            selfDescriptionRepository.createSelfDescription(configInstance.getRemoteAasLocation(),
                    SelfDescriptionRepository.SelfDescriptionSourceType.SERVICE);
        }

        if (Objects.isNull(configInstance.getLocalAasModelPath())) {
            return;
        }

        Path aasConfigPath = null;

        if (Objects.nonNull(configInstance.getAasServiceConfigPath())) {
            aasConfigPath = Path.of(configInstance.getAasServiceConfigPath());
        }

        URL serviceUrl;
        try {
            serviceUrl = aasController.startService(
                    Path.of(configInstance.getLocalAasModelPath()),
                    configInstance.getLocalAasServicePort(),
                    aasConfigPath);

        } catch (IOException startAssetAdministrationShellException) {
            throw new EdcException("Could not start AAS service provided by configuration",
                    startAssetAdministrationShellException);
        }

        selfDescriptionRepository.createSelfDescription(serviceUrl,
                SelfDescriptionRepository.SelfDescriptionSourceType.SERVICE);
    }

    @Override
    public void shutdown() {
        // Gracefully stop AAS services
        aasController.stopServices();
    }

}

