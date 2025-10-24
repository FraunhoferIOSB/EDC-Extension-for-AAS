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

import de.fraunhofer.iosb.aas.lib.model.impl.Registry;
import de.fraunhofer.iosb.aas.lib.model.impl.Service;
import de.fraunhofer.iosb.api.PublicApiManagementService;
import de.fraunhofer.iosb.api.model.HttpMethod;
import de.fraunhofer.iosb.app.aas.agent.impl.RegistryAgent;
import de.fraunhofer.iosb.app.aas.agent.impl.ServiceAgent;
import de.fraunhofer.iosb.app.aas.mapper.environment.EnvironmentToAssetMapper;
import de.fraunhofer.iosb.app.controller.AasController;
import de.fraunhofer.iosb.app.controller.ConfigurationController;
import de.fraunhofer.iosb.app.controller.SelfDescriptionController;
import de.fraunhofer.iosb.app.edc.CleanUpService;
import de.fraunhofer.iosb.app.edc.asset.AssetRegistrar;
import de.fraunhofer.iosb.app.edc.contract.ContractRegistrar;
import de.fraunhofer.iosb.app.model.aas.registry.RegistryRepository;
import de.fraunhofer.iosb.app.model.aas.registry.RegistryRepositoryUpdater;
import de.fraunhofer.iosb.app.model.aas.service.ServiceRepository;
import de.fraunhofer.iosb.app.model.aas.service.ServiceRepositoryUpdater;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.app.pipeline.Pipeline;
import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import de.fraunhofer.iosb.app.pipeline.helper.CollectionFeeder;
import de.fraunhofer.iosb.app.pipeline.helper.Filter;
import de.fraunhofer.iosb.app.pipeline.helper.InputOutputZipper;
import de.fraunhofer.iosb.app.pipeline.helper.MapValueProcessor;
import de.fraunhofer.iosb.app.sync.Synchronizer;
import de.fraunhofer.iosb.app.util.InetTools;
import de.fraunhofer.iosb.app.util.VariableRateScheduler;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.jsonld.spi.JsonLd;
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
import java.util.function.Function;

import static de.fraunhofer.iosb.app.controller.SelfDescriptionController.SELF_DESCRIPTION_PATH;
import static de.fraunhofer.iosb.app.pipeline.PipelineFailure.Type.FATAL;
import static de.fraunhofer.iosb.app.util.InetTools.getSelfSignedCertificate;
import static de.fraunhofer.iosb.app.util.InetTools.isConnectionTrusted;
import static de.fraunhofer.iosb.constants.AasConstants.AAS_PREFIX;
import static de.fraunhofer.iosb.constants.AasConstants.AAS_V30_NAMESPACE;

/**
 * EDC Extension supporting usage of Asset Administration Shells.
 */
@Extension(value = AasExtension.NAME)
public class AasExtension implements ServiceExtension {

    public static final String NAME = "EDC4AAS Extension";

    private static final String SETTINGS_PREFIX = "edc.aas";
    @Inject // Register public endpoints
    private PublicApiManagementService publicApiManagementService;
    @Inject
    private AssetIndex assetIndex;
    @Inject
    private ContractDefinitionStore contractDefinitionStore;
    @Inject
    private EdcHttpClient edcHttpClient;
    @Inject // Create / manage EDC policies
    private PolicyDefinitionStore policyDefinitionStore;
    @Inject // Register http endpoint at EDC
    private WebService webService;
    @Inject // Add AAS namespace to JSON LD context
    private JsonLd jsonLd;
    private AasController aasController;
    private Monitor monitor;
    private VariableRateScheduler servicePipeline;
    private VariableRateScheduler registryPipeline;
    private String participantId;
    private RegistryRepository registryRepository;
    private ServiceRepository serviceRepository;

    @Override
    public void initialize(ServiceExtensionContext context) {
        jsonLd.registerNamespace(AAS_PREFIX, AAS_V30_NAMESPACE);

        this.monitor = context.getMonitor().withPrefix(NAME);
        webService.registerResource(new ConfigurationController(context.getConfig(SETTINGS_PREFIX), monitor));
        aasController = new AasController(monitor);

        serviceRepository = new ServiceRepository();
        registryRepository = new RegistryRepository();

        // Add public endpoint if wanted by config
        if (Configuration.getInstance().isExposeSelfDescription()) {
            publicApiManagementService.addEndpoints(List.of(new de.fraunhofer.iosb.api.model.Endpoint(SELF_DESCRIPTION_PATH, HttpMethod.GET,
                    Map.of())));
        }

        webService.registerResource(new SelfDescriptionController(monitor, serviceRepository, registryRepository));
        webService.registerResource(new Endpoint(serviceRepository, registryRepository, aasController, monitor));

        this.participantId = context.getParticipantId();
    }

    @Override
    public void start() {
        // This is to allow for self-signed services
        serviceRepository.registerListener(aasController);
        registryRepository.registerListener(aasController);

        var serviceSynchronization = new Pipeline.Builder<Void, Void>()
                .monitor(monitor.withPrefix("Service Pipeline"))
                .supplier(serviceRepository::getAll)
                .step(new Filter<>(InetTools::pingHost, "Connection Test"))
                .step(new InputOutputZipper<>(new ServiceAgent(edcHttpClient, monitor), Function.identity()))
                .step(new EnvironmentToAssetMapper())
                .step(new CollectionFeeder<>(new ServiceRepositoryUpdater(serviceRepository)))
                .step(new Synchronizer())
                .step(new AssetRegistrar(assetIndex, monitor.withPrefix("Service Pipeline")))
                .step(new ContractRegistrar(contractDefinitionStore, policyDefinitionStore, monitor, participantId))
                .build();

        servicePipeline = new VariableRateScheduler(1, serviceSynchronization, monitor);
        servicePipeline.scheduleAtVariableRate(() -> Configuration.getInstance().getSyncPeriod());
        serviceRepository.registerListener(servicePipeline);

        var registrySynchronization = new Pipeline.Builder<Void, Void>()
                .monitor(monitor.withPrefix("Registry Pipeline"))
                .supplier(registryRepository::getAll)
                .step(new Filter<>(InetTools::pingHost, "Connection Test"))
                .step(new InputOutputZipper<>(new RegistryAgent(edcHttpClient, monitor),
                        Function.identity()))
                .step(new MapValueProcessor<>(
                        new EnvironmentToAssetMapper(),
                        // Remove fatal results from further processing
                        result -> result.failed() && FATAL.equals(result.getFailure().getFailureType()) ? null : result)
                )
                .step(PipelineStep.create(registriesMap -> (Collection<Registry>) registriesMap.entrySet().stream()
                        .map(registry -> registry.getKey().with(registry.getValue()))
                        .toList()))
                .step(new RegistryRepositoryUpdater(registryRepository))
                .step(new Synchronizer())
                .step(new AssetRegistrar(assetIndex, monitor.withPrefix("Registry Pipeline")))
                .step(new ContractRegistrar(contractDefinitionStore, policyDefinitionStore, monitor, participantId))
                .build();

        registryPipeline = new VariableRateScheduler(1, registrySynchronization, monitor);
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
        registerAasServicesByConfig(serviceRepository);

        monitor.info("AAS Extension started.");
    }

    private void registerAasServicesByConfig(ServiceRepository serviceRepository) {
        var configInstance = Configuration.getInstance();

        if (Objects.nonNull(configInstance.getRemoteAasLocation())) {
            serviceRepository.create(new Service.Builder()
                    .withUrl(configInstance.getRemoteAasLocation())
                    .build());
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
            if (configInstance.getLocalAasServicePort() == 0 && aasConfigPath == null) {
                serviceUrl = aasController.startService(Path.of(configInstance.getLocalAasModelPath()));
            } else {
                serviceUrl = aasController.startService(
                        Path.of(configInstance.getLocalAasModelPath()),
                        configInstance.getLocalAasServicePort(),
                        aasConfigPath);
            }
        } catch (Exception startAssetAdministrationShellException) {
            monitor.severe("Could not start / register AAS service provided by configuration.\nReason: %s %s"
                    .formatted(startAssetAdministrationShellException.getMessage(), startAssetAdministrationShellException.getCause()));
            return;
        }

        // Now, check if the created service
        // - has a valid certificate OR
        // - has a self-signed one AND we accept self-signed
        if (isConnectionTrusted(serviceUrl) || (configInstance.isAllowSelfSignedCertificates() && getSelfSignedCertificate(serviceUrl).succeeded())) {
            serviceRepository.create(new Service.Builder()
                    .withUrl(serviceUrl)
                    .build());
        } else {
            aasController.stopService(serviceUrl);
            monitor.severe("AAS service uses self-signed (not allowed) or otherwise invalid certificate.");
        }
    }

    @Override
    public void shutdown() {
        // Stop pipelines
        registryPipeline.terminate();
        servicePipeline.terminate();
        // Gracefully stop AAS services
        aasController.stopServices();
    }
}

