package de.fraunhofer.iosb.app.sync;

import static java.lang.String.format;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.edc.spi.EdcException;

import de.fraunhofer.iosb.app.controller.AasController;
import de.fraunhofer.iosb.app.controller.ResourceController;
import de.fraunhofer.iosb.app.model.aas.AASElement;
import de.fraunhofer.iosb.app.model.aas.CustomAssetAdministrationShellEnvironment;
import de.fraunhofer.iosb.app.model.aas.CustomSubmodel;
import de.fraunhofer.iosb.app.model.aas.CustomSubmodelElement;
import de.fraunhofer.iosb.app.model.aas.IdsAssetElement;
import de.fraunhofer.iosb.app.model.ids.SelfDescription;
import de.fraunhofer.iosb.app.util.AASUtil;
import io.adminshell.aas.v3.dataformat.DeserializationException;
import jakarta.ws.rs.core.MediaType;

public class Synchronizer {

    private final Map<URL, SelfDescription> selfDescriptionRepository;
    private final AasController aasController;
    private final ResourceController resourceController;

    public Synchronizer(Map<URL, SelfDescription> selfDescriptionRepository,
            AasController aasController, ResourceController resourceController) {
        this.selfDescriptionRepository = selfDescriptionRepository;
        this.aasController = aasController;
        this.resourceController = resourceController;
    }

    /**
     * Synchronize AAS element structure with EDC AssetIndex
     * 
     * @param aasServiceUrl AAS service URL
     */
    public void synchronize() {
        for (Entry<URL, SelfDescription> selfDescription : selfDescriptionRepository.entrySet()) {
            synchronize(selfDescription.getKey(), selfDescription.getValue());
        }
    }

    private void synchronize(URL aasServiceUrl, SelfDescription selfDescriptionToUpdate) {
        var oldSelfDescription = selfDescriptionRepository.get(aasServiceUrl);
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

        if (Objects.isNull(oldSelfDescription)) {
            initSelfDescriptionAndAssetIndex(aasServiceUrl, newEnvironment);
            return;
        }

        var oldEnvironment = oldSelfDescription.getEnvironment();

        // For all shells, conceptDescriptions, submodels, submodelElements:
        // Check whether any were added or removed.
        // If a new element was added: This element is now in newEnvironment without
        // idsContractId/idsAssetId
        // If the element exists in oldEnvironment, copy the old elements into
        // newEnvironment, already having an idsContractId/idsAssetId
        syncShell(newEnvironment, oldEnvironment);

        syncSubmodel(newEnvironment, oldEnvironment);

        syncConceptDescription(newEnvironment, oldEnvironment);

        // All elements that are new are now added to the EDC
        // AssetIndex/ContractDefinitionStore
        addAssetsContracts(AASUtil.getAllElements(newEnvironment).stream()
                .filter(element -> Objects.isNull(element.getIdsContractId())).collect(Collectors.toList()));

        var oldElements = AASUtil.getAllElements(oldEnvironment);
        // Important: Equality is when identification & idShort & containing elements
        // are equal
        oldElements.removeAll(AASUtil.getAllElements(newEnvironment));
        removeAssetsContracts(oldElements);

        // Finally, update the self description
        selfDescriptionRepository.put(aasServiceUrl, new SelfDescription(newEnvironment));
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
                .filter(element -> allOldElements.contains(element))
                .forEach(element -> {
                    var oldElement = allOldElements.stream()
                            .filter(oldElementTest -> oldElementTest.equals(element)).findFirst().orElse(element);
                    element.setIdsAssetId(oldElement.getIdsAssetId());
                    element.setIdsContractId(oldElement.getIdsContractId());
                });
    }

    private void initSelfDescriptionAndAssetIndex(URL aasServiceUrl,
            CustomAssetAdministrationShellEnvironment newEnvironment) {
        addAssetsContracts(AASUtil.getAllElements(newEnvironment));
        selfDescriptionRepository.put(aasServiceUrl, new SelfDescription(newEnvironment));
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

    /*
     * Removes any EDC asset and EDC contract off the EDC
     * AssetIndex/ContractDefinitionStore given a list of AAS elements.
     */
    private void removeAssetsContracts(List<? extends IdsAssetElement> elements) {
        elements.forEach(element -> {
            resourceController.deleteAssetAndContracts(element.getIdsAssetId());
            resourceController.deleteContract(element.getIdsContractId());
        });
    }
}
