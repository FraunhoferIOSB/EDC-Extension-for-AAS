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
import de.fraunhofer.iosb.app.aas.agent.impl.RegistryAgent;
import de.fraunhofer.iosb.app.aas.agent.impl.ServiceAgent;
import de.fraunhofer.iosb.app.controller.AasController;
import de.fraunhofer.iosb.app.controller.ConfigurationController;
import de.fraunhofer.iosb.app.controller.SelfDescriptionController;
import de.fraunhofer.iosb.app.edc.CleanUpService;
import de.fraunhofer.iosb.app.edc.asset.AssetRegistrar;
import de.fraunhofer.iosb.app.edc.contract.ContractRegistrar;
import de.fraunhofer.iosb.app.model.aas.AasAccessUrl;
import de.fraunhofer.iosb.app.model.aas.registry.Registry;
import de.fraunhofer.iosb.app.model.aas.registry.RegistryRepository;
import de.fraunhofer.iosb.app.model.aas.registry.RegistryRepositoryUpdater;
import de.fraunhofer.iosb.app.model.aas.service.ServiceRepository;
import de.fraunhofer.iosb.app.model.aas.service.ServiceRepositoryUpdater;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.app.pipeline.Pipeline;
import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import de.fraunhofer.iosb.app.pipeline.helper.CollectionFeeder;
import de.fraunhofer.iosb.app.pipeline.helper.InputOutputZipper;
import de.fraunhofer.iosb.app.pipeline.helper.MapValueProcessor;
import de.fraunhofer.iosb.app.sync.Synchronizer;
import de.fraunhofer.iosb.app.util.VariableRateScheduler;
import de.fraunhofer.iosb.registry.AasServiceRegistry;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;

import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static de.fraunhofer.iosb.app.controller.SelfDescriptionController.SELF_DESCRIPTION_PATH;
import static de.fraunhofer.iosb.app.pipeline.PipelineFailure.Type.FATAL;
import static de.fraunhofer.iosb.app.util.InetTools.pingHost;

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
    private AasServiceRegistry foreignServerRegistry;
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
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        this.monitor = context.getMonitor().withPrefix(NAME);
        webService.registerResource(new ConfigurationController(context.getConfig(SETTINGS_PREFIX), monitor));

        aasController = new AasController(foreignServerRegistry, monitor);
        var serviceRepository = new ServiceRepository();
        var registryRepository = new RegistryRepository();

        // This is to allow for self-signed services
        serviceRepository.registerListener(aasController);
        registryRepository.registerListener(aasController);

        // Check if a URL is reachable by pinging the host+port combination (not actual ICMP)
        PipelineStep<URL, URL> reachabilityCheck = PipelineStep.create(url -> {
            if (!pingHost(url.getHost(), url.getPort(), 10)) {
                monitor.severe("URL %s not reachable!".formatted(url));
                return null;
            }
            return url;
        });

        var serviceSynchronization = new Pipeline.Builder<Void, Void>()
                .monitor(monitor.withPrefix("Service Synchronization Pipeline"))
                .supplier(serviceRepository::getAllServiceAccessUrls)
                .step(new CollectionFeeder<>(reachabilityCheck))
                .step(new InputOutputZipper<>(new ServiceAgent(aasDataProcessorFactory), AasAccessUrl::new))
                .step(new EnvironmentToAssetMapper(() -> Configuration.getInstance().isOnlySubmodels()))
                .step(new CollectionFeeder<>(new ServiceRepositoryUpdater(serviceRepository)))
                .step(new Synchronizer())
                .step(new AssetRegistrar(assetIndex, monitor))
                .step(new ContractRegistrar(contractDefinitionStore, policyDefinitionStore, monitor))
                .build();

        var servicePipeline = new VariableRateScheduler(1, serviceSynchronization, monitor);
        servicePipeline.scheduleAtVariableRate(() -> Configuration.getInstance().getSyncPeriod());
        serviceRepository.registerListener(servicePipeline);

        var registrySynchronization = new Pipeline.Builder<Void, Void>()
                .monitor(monitor.withPrefix("Registry Synchronization Pipeline"))
                .supplier(registryRepository::getAllUrls)
                .step(new CollectionFeeder<>(reachabilityCheck))
                .step(new InputOutputZipper<>(new RegistryAgent(aasDataProcessorFactory, foreignServerRegistry), AasAccessUrl::new))
                .step(new MapValueProcessor<>(
                        new EnvironmentToAssetMapper(() -> Configuration.getInstance().isOnlySubmodels()),
                        // Remove fatal results from further processing
                        result -> result.failed() && FATAL.equals(result.getFailure().getFailureType()) ? null : result)
                )
                .step(PipelineStep.create(registriesMap -> (Collection<Registry>) registriesMap.entrySet().stream()
                        .map(registry -> new Registry(registry.getKey(), registry.getValue()))
                        .toList()))
                .step(new RegistryRepositoryUpdater(registryRepository))
                .step(new Synchronizer())
                .step(new AssetRegistrar(assetIndex, monitor))
                .step(new ContractRegistrar(contractDefinitionStore, policyDefinitionStore, monitor))
                .build();

        var registryPipeline = new VariableRateScheduler(1, registrySynchronization, monitor);
        registryPipeline.scheduleAtVariableRate(() -> Configuration.getInstance().getSyncPeriod());
        registryRepository.registerListener(registryPipeline);

        // Clean up assets and contracts if service/registry is unregistered
        var cleanUpService = CleanUpService.Builder.newInstance()
                .assetIndex(assetIndex)
                .policyDefinitionStore(policyDefinitionStore)
                .monitor(monitor)
                .contractDefinitionStore(contractDefinitionStore)
                .build();

        serviceRepository.registerListener(cleanUpService);
        registryRepository.registerListener(cleanUpService);

        // Add public endpoint if wanted by config
        if (Configuration.getInstance().isExposeSelfDescription()) {
            publicApiManagementService.addEndpoints(List.of(new de.fraunhofer.iosb.api.model.Endpoint(SELF_DESCRIPTION_PATH, HttpMethod.GET, Map.of())));
        }

        webService.registerResource(new SelfDescriptionController(monitor, serviceRepository, registryRepository));
        webService.registerResource(new Endpoint(serviceRepository, registryRepository, aasController, monitor));

        registerAasServicesByConfig(serviceRepository);
    }

    private void registerAasServicesByConfig(ServiceRepository selfDescriptionRepository) {
        var configInstance = Configuration.getInstance();

        if (Objects.nonNull(configInstance.getRemoteAasLocation())) {
            selfDescriptionRepository.create(configInstance.getRemoteAasLocation());
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
        } catch (Exception startAssetAdministrationShellException) {
            monitor.severe("Could not start AAS service provided by configuration",
                    startAssetAdministrationShellException);
            return;
        }

        selfDescriptionRepository.create(serviceUrl);
    }

    @Override
    public void shutdown() {
        // Gracefully stop AAS services
        aasController.stopServices();
    }
}

