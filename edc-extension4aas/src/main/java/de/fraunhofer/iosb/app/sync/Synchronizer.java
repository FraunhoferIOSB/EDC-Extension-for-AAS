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
package de.fraunhofer.iosb.app.sync;

import de.fraunhofer.iosb.app.controller.AasController;
import de.fraunhofer.iosb.app.controller.ResourceController;
import de.fraunhofer.iosb.app.model.aas.AssetAdministrationShellElement;
import de.fraunhofer.iosb.app.model.aas.CustomAssetAdministrationShellEnvironment;
import de.fraunhofer.iosb.app.model.aas.CustomSubmodel;
import de.fraunhofer.iosb.app.model.aas.CustomSubmodelElement;
import de.fraunhofer.iosb.app.model.aas.IdsAssetElement;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionChangeListener;
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository;
import de.fraunhofer.iosb.app.util.AssetAdministrationShellUtil;
import de.fraunhofer.iosb.registry.AasServiceRegistry;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;


/**
 * Synchronize registered AAS services with local
 * self-descriptions and assetIndex/contractStore.
 */
public class Synchronizer implements SelfDescriptionChangeListener {

    // Main objective of Synchronizer is making sure SelfDescription and EDC asset store state are valid
    private final SelfDescriptionRepository selfDescriptionRepository;
    private final ResourceController resourceController;
    // Communication with AAS of any type
    private final AasController aasController;

    private final boolean onlySubmodels;
    private final AasServiceRegistry aasServiceRegistry;

    private Synchronizer(SelfDescriptionRepository selfDescriptionRepository,
                         AasController aasController,
                         AssetIndex assetIndex,
                         ContractDefinitionStore contractStore,
                         PolicyDefinitionStore policyStore,
                         Monitor monitor,
                         AasServiceRegistry aasServiceRegistry) {
        this.selfDescriptionRepository = selfDescriptionRepository;
        this.aasController = aasController;
        this.aasServiceRegistry = aasServiceRegistry;

        this.onlySubmodels = Configuration.getInstance().isOnlySubmodels();
        this.resourceController = new ResourceController(assetIndex, contractStore, policyStore, monitor);
    }

    /**
     * Synchronize AAS services with self-description and EDC
     * AssetIndex/ContractStore
     */
    public void synchronize() {
        for (var selfDescription : selfDescriptionRepository.getAllSelfDescriptions().keySet()) {
            try {
                synchronize(new URL(selfDescription));
            } catch (MalformedURLException e) {
                throw new EdcException("AAS URL malformed while synchronizing", e);
            }
        }
    }

    private void synchronize(URL aasServiceUrl) {

        var oldSelfDescription = selfDescriptionRepository.getSelfDescription(aasServiceUrl);

        var oldEnvironment = Objects.isNull(oldSelfDescription) ?
                new CustomAssetAdministrationShellEnvironment() :
                oldSelfDescription.getEnvironment();

        var newEnvironment = fetchCurrentAasModel(aasServiceUrl);

        // Check whether any element was added or removed.
        // - Added elements need idsContractId/idsAssetId
        // - Existing elements are copied into newEnvironment
        syncShell(newEnvironment, oldEnvironment);
        syncConceptDescription(newEnvironment, oldEnvironment);
        syncSubmodel(newEnvironment, oldEnvironment);

        addNewElements(newEnvironment);
        selfDescriptionRepository.updateSelfDescription(aasServiceUrl, newEnvironment);
    }

    private CustomAssetAdministrationShellEnvironment fetchCurrentAasModel(URL aasServiceUrl) {
        CustomAssetAdministrationShellEnvironment newEnvironment;

        try { // Fetch current AAS model from AAS service
            newEnvironment = aasController.getAasModelWithUrls(aasServiceUrl, onlySubmodels);
        } catch (IOException aasServiceUnreachableException) {
            throw new EdcException(format("Could not reach AAS service (%s): %s", aasServiceUrl,
                    aasServiceUnreachableException.getMessage()), aasServiceUnreachableException);
        }
        return newEnvironment;
    }

    private void addNewElements(CustomAssetAdministrationShellEnvironment newEnvironment) {
        var envElements = AssetAdministrationShellUtil.getAllElements(newEnvironment);
        addAssetsContracts(envElements.stream().filter(
                        element -> Objects.isNull(element.getIdsAssetId()) || Objects.isNull(element.getIdsContractId()))
                .toList());
    }

    private void syncShell(CustomAssetAdministrationShellEnvironment newEnvironment,
                           CustomAssetAdministrationShellEnvironment oldEnvironment) {
        var oldShells = oldEnvironment.getAssetAdministrationShells();
        newEnvironment.getAssetAdministrationShells().replaceAll(
                shell -> oldShells.contains(shell)
                        ? oldShells.get(oldShells.indexOf(shell))
                        : shell);
    }

    private void syncConceptDescription(CustomAssetAdministrationShellEnvironment newEnvironment,
                                        CustomAssetAdministrationShellEnvironment oldEnvironment) {
        var oldConceptDescriptions = oldEnvironment.getConceptDescriptions();
        newEnvironment.getConceptDescriptions().replaceAll(
                conceptDescription -> oldConceptDescriptions.contains(conceptDescription)
                        ? oldConceptDescriptions.get(oldConceptDescriptions.indexOf(conceptDescription))
                        : conceptDescription);
    }

    private void syncSubmodel(CustomAssetAdministrationShellEnvironment newEnvironment,
                              CustomAssetAdministrationShellEnvironment oldEnvironment) {
        var oldSubmodels = oldEnvironment.getSubmodels();
        newEnvironment.getSubmodels().forEach(submodel -> {
            CustomSubmodel oldSubmodel;
            if (oldSubmodels.contains(submodel)) {
                oldSubmodel = oldSubmodels.get(oldSubmodels.indexOf(submodel));
            } else {
                oldSubmodel = oldSubmodels.stream().filter(
                                submodel::equals)
                        .findFirst().orElse(null);
                if (Objects.isNull(oldSubmodel)) {
                    return;
                }
            }

            submodel.setIdsAssetId(oldSubmodel.getIdsAssetId());
            submodel.setIdsContractId(oldSubmodel.getIdsContractId());
            var allElements = AssetAdministrationShellUtil.getAllSubmodelElements(submodel);
            var allOldElements = AssetAdministrationShellUtil.getAllSubmodelElements(oldSubmodel);
            syncSubmodelElements(allElements, allOldElements);
        });
    }

    private void syncSubmodelElements(Collection<CustomSubmodelElement> allElements,
                                      Collection<CustomSubmodelElement> allOldElements) {
        allElements.stream()
                .filter(allOldElements::contains)
                .forEach(element -> {
                    var oldElement = allOldElements.stream()
                            .filter(oldElementTest -> oldElementTest.equals(element)).findFirst().orElse(element);
                    element.setIdsAssetId(oldElement.getIdsAssetId());
                    element.setIdsContractId(oldElement.getIdsContractId());
                });
    }

    private void addAssetsContracts(List<? extends IdsAssetElement> elements) {
        // Add each AAS element to EDC AssetIndex, giving it a contract
        elements.forEach(element -> {
            // Version unknown, MediaType is "application/json" by default
            var assetContractPair = resourceController.createResource(element.getSourceUrl(),
                    element.getReferenceChain(),
                    ((AssetAdministrationShellElement) element).getIdShort(),
                    MediaType.APPLICATION_JSON);
            element.setIdsAssetId(assetContractPair.first());
            element.setIdsContractId(assetContractPair.second());
        });
    }

    @Override
    public void created(URL aasUrl) {
        var registrationResult = aasServiceRegistry.register(aasUrl.toString());
        if (registrationResult.failed()) {
            throw new EdcException(format("Could not synchronize with %s: %s",
                    aasUrl,
                    registrationResult.getFailureMessages()));
        }

        synchronize(aasUrl);
    }

    @Override
    public void removed(URL removed) {
        AssetAdministrationShellUtil.getAllElements(selfDescriptionRepository.getSelfDescription(removed).getEnvironment())
                .forEach(element -> resourceController.deleteAssetAndContracts(element.getIdsAssetId()));

        aasServiceRegistry.unregister(removed.toString());
    }

    public static class SynchronizerBuilder {
        private SelfDescriptionRepository selfDescriptionRepository;
        private AasController aasController;
        private AssetIndex assetIndex;
        private ContractDefinitionStore contractStore;
        private PolicyDefinitionStore policyStore;
        private Monitor monitor;
        private AasServiceRegistry aasServiceRegistry;

        public SynchronizerBuilder selfDescriptionRepository(SelfDescriptionRepository selfDescriptionRepository) {
            this.selfDescriptionRepository = selfDescriptionRepository;
            return this;
        }

        public SynchronizerBuilder aasController(AasController aasController) {
            this.aasController = aasController;
            return this;
        }

        public SynchronizerBuilder assetIndex(AssetIndex assetIndex) {
            this.assetIndex = assetIndex;
            return this;
        }

        public SynchronizerBuilder contractStore(ContractDefinitionStore contractStore) {
            this.contractStore = contractStore;
            return this;
        }

        public SynchronizerBuilder policyStore(PolicyDefinitionStore policyStore) {
            this.policyStore = policyStore;
            return this;
        }

        public SynchronizerBuilder monitor(Monitor monitor) {
            this.monitor = monitor;
            return this;
        }

        public SynchronizerBuilder aasServiceRegistry(AasServiceRegistry aasServiceRegistry) {
            this.aasServiceRegistry = aasServiceRegistry;
            return this;
        }

        public Synchronizer build() {
            Objects.requireNonNull(selfDescriptionRepository, "selfDescriptionRepository");
            Objects.requireNonNull(aasController, "aasController");
            Objects.requireNonNull(assetIndex, "assetIndex");
            Objects.requireNonNull(contractStore, "contractStore");
            Objects.requireNonNull(policyStore, "policyStore");
            Objects.requireNonNull(monitor, "monitor");
            Objects.requireNonNull(aasServiceRegistry, "aasServiceRegistry");
            return new Synchronizer(selfDescriptionRepository, aasController, assetIndex, contractStore, policyStore, monitor, aasServiceRegistry);
        }
    }
}
