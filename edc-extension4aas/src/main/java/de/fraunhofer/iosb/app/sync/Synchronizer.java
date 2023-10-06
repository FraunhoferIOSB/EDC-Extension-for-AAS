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
import de.fraunhofer.iosb.app.model.aas.*;
import de.fraunhofer.iosb.app.model.ids.SelfDescription;
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionChangeListener;
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository;
import de.fraunhofer.iosb.app.util.AASUtil;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.DeserializationException;
import org.eclipse.edc.spi.EdcException;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;

/**
 * Synchronize registered AAS services with local self descriptions and
 * assetIndex/contractStore.
 */
public class Synchronizer implements SelfDescriptionChangeListener {

    private final SelfDescriptionRepository selfDescriptionRepository;
    private final AasController aasController;
    private final ResourceController resourceController;

    public Synchronizer(SelfDescriptionRepository selfDescriptionRepository,
                        AasController aasController, ResourceController resourceController) {
        this.selfDescriptionRepository = selfDescriptionRepository;
        this.aasController = aasController;
        this.resourceController = resourceController;
    }

    /**
     * Synchronize AAS services with self description and EDC
     * AssetIndex/ContractStore
     */
    public void synchronize() {
        for (var selfDescription : selfDescriptionRepository.getAllSelfDescriptions()) {
            synchronize(selfDescription.getKey());
        }
    }

    private void synchronize(URL aasServiceUrl) {
        var oldSelfDescription = selfDescriptionRepository.getSelfDescription(aasServiceUrl);
        CustomAssetAdministrationShellEnvironment newEnvironment;

        newEnvironment = fetchCurrentAasModel(aasServiceUrl);

        if (Objects.nonNull(oldSelfDescription)) {
            var oldEnvironment = oldSelfDescription.getEnvironment();

            // For all shells, conceptDescriptions, submodels, submodelElements:
            // Check whether any element was added or removed.
            // If a new element was added: This element is now in newEnvironment without
            // idsContractId/idsAssetId field.
            // If the element exists in oldEnvironment, copy the old elements into
            // newEnvironment, already having an idsContractId/idsAssetId
            syncShell(newEnvironment, oldEnvironment);
            syncSubmodel(newEnvironment, oldEnvironment);
            syncConceptDescription(newEnvironment, oldEnvironment);

            removeOldElements(newEnvironment, oldEnvironment);

            // Finally, update the self description
        }
        addNewElements(newEnvironment);
        selfDescriptionRepository.updateSelfDescription(aasServiceUrl, newEnvironment);
    }

    private CustomAssetAdministrationShellEnvironment fetchCurrentAasModel(URL aasServiceUrl) {
        CustomAssetAdministrationShellEnvironment newEnvironment;

        try { // Fetch current AAS model from AAS service
            newEnvironment = aasController.getAasModelWithUrls(aasServiceUrl);
        } catch (IOException aasServiceUnreachableException) {
            throw new EdcException(format("Could not reach AAS service (%s): %s", aasServiceUrl,
                    aasServiceUnreachableException.getMessage()));
        } catch (DeserializationException aasModelDeserializationException) {
            throw new EdcException(format("Could not deserialize AAS model (%s): %s", aasServiceUrl,
                    aasModelDeserializationException.getMessage()));
        }
        return newEnvironment;
    }

    private void addNewElements(CustomAssetAdministrationShellEnvironment newEnvironment) {
        var envElements = AASUtil.getAllElements(newEnvironment);
        addAssetsContracts(envElements.stream().filter(element -> Objects.isNull(element.getIdsContractId()))
                .toList());
    }

    /*
     * Removes elements that were deleted on AAS service
     */
    private void removeOldElements(CustomAssetAdministrationShellEnvironment newEnvironment,
                                   CustomAssetAdministrationShellEnvironment oldEnvironment) {
        var elementsToRemove = AASUtil.getAllElements(oldEnvironment);
        elementsToRemove.removeAll(AASUtil.getAllElements(newEnvironment));
        removeAssetsContracts(elementsToRemove);
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
                oldSubmodel = oldSubmodels
                        .get(oldSubmodels.indexOf(submodel));
            } else {
                oldSubmodel = oldSubmodels.stream().filter(
                                oldSubmodelTest -> oldSubmodelTest.getIdentification().equals(submodel.getIdentification())
                                        && oldSubmodelTest.getIdShort().equals(submodel.getIdShort()))
                        .findFirst().orElse(null);
                if (Objects.isNull(oldSubmodel)) {
                    return;
                }
            }

            submodel.setIdsAssetId(oldSubmodel.getIdsAssetId());
            submodel.setIdsContractId(oldSubmodel.getIdsContractId());
            var allElements = AASUtil.getAllSubmodelElements(submodel);
            var allOldElements = AASUtil.getAllSubmodelElements(oldSubmodel);
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
                    ((AASElement) element).getIdShort(),
                    MediaType.APPLICATION_JSON, null);
            element.setIdsAssetId(assetContractPair.getFirst());
            element.setIdsContractId(assetContractPair.getSecond());
        });
    }

    private void removeAssetsContracts(List<? extends IdsAssetElement> elements) {
        elements.forEach(element -> resourceController.deleteAssetAndContracts(element.getIdsAssetId()));
    }

    @Override
    public void created(URL aasUrl) {
        synchronize(aasUrl);
    }

    @Override
    public void removed(SelfDescription removed) {
        var allElements = AASUtil.getAllElements(removed.getEnvironment());
        removeAssetsContracts(allElements);
    }

}
