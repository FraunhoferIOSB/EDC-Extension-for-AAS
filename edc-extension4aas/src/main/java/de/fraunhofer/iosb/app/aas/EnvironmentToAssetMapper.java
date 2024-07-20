package de.fraunhofer.iosb.app.aas;

import de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress;
import org.eclipse.digitaltwin.aas4j.v3.model.*;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAdministrativeInformation;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultKey;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import java.net.URL;
import java.util.*;

/**
 * Create a mapping from an AAS environment to EDC assets.
 * This is not a holistic transformation but rather maps some
 * key elements and creates appropriate data address and assetId.
 * TODO maybe find a better name
 */
public class EnvironmentToAssetMapper {
    private final URL accessUrl;
    private final boolean onlySubmodels;

    public EnvironmentToAssetMapper(URL accessUrl, boolean onlySubmodels) {
        this.accessUrl = accessUrl;
        this.onlySubmodels = onlySubmodels;
    }

    /**
     * Create a nested EDC asset from this environment structure. The top level asset is just to hold the shells,
     * submodels and concept descriptions and should not be added to assetIndex.
     *
     * @param environment AAS environment to transform to asset
     * @return Asset as described above
     */
    public Asset map(Environment environment) {
        var assetBuilder = Asset.Builder.newInstance().property("submodels",
                environment.getSubmodels().stream().map(this::mapSubmodelToAsset).toList());
        if (onlySubmodels) {
            return assetBuilder.build();
        }
        return assetBuilder
                .property("shells",
                        environment.getAssetAdministrationShells().stream().map(this::mapShellToAsset).toList())
                .property("conceptDescriptions",
                        environment.getConceptDescriptions().stream().map(this::mapConceptDescriptionToAsset).toList())
                .build();
    }

    private <T extends Referable> Asset.Builder mapReferableToAssetBuilder(T referable) {
        return Asset.Builder.newInstance()
                .properties(Map.of(
                        "idShort", referable.getIdShort(),
                        "name", referable.getDisplayName(),
                        "description", referable.getDescription()));
    }

    private <T extends Identifiable> Asset.Builder mapIdentifiableToAssetBuilder(T identifiable) {
        var admin = Optional.ofNullable(identifiable.getAdministration())
                .orElse(new DefaultAdministrativeInformation.Builder().build());
        var version = null != admin.getVersion() && null != admin.getRevision() ?
                String.valueOf(admin.getVersion()).concat(":").concat(String.valueOf(admin.getRevision())) : null;

        return mapReferableToAssetBuilder(identifiable)
                .id(String.valueOf(identifiable.getId().hashCode()))
                .version(version)
                .contentType("application/json")
                .properties(Map.of(
                        "id", identifiable.getId(),
                        "embeddedDataSpecifications", admin.getEmbeddedDataSpecifications()));
    }

    /* May contain traces of recursion */
    private <T extends SubmodelElement> Asset mapSubmodelElementToAsset(Reference parentReference, T submodelElement) {
        var reference = createReference(submodelElement.getIdShort(), parentReference);

        var children = getContainerElements(submodelElement).stream()
                .map(elem -> mapSubmodelElementToAsset(reference, elem))
                .toList();

        return mapReferableToAssetBuilder(submodelElement)
                .id(String.valueOf(submodelElement.getIdShort().hashCode()))
                .contentType("application/json")
                .properties(Map.of(
                        "embeddedDataSpecifications", submodelElement.getEmbeddedDataSpecifications(),
                        "semanticId", submodelElement.getSemanticId(),
                        "value", children))
                .dataAddress(AasDataAddress.Builder.newInstance()
                        .baseUrl(accessUrl.toString())
                        .referenceChain(reference)
                        .build())
                .build();
    }

    private <T extends SubmodelElement> Collection<SubmodelElement> getContainerElements(T submodelElement) {
        if (submodelElement instanceof SubmodelElementCollection) {
            return ((SubmodelElementCollection) submodelElement).getValue();
        } else if (submodelElement instanceof SubmodelElementList) {
            return ((SubmodelElementList) submodelElement).getValue();
        } else { // Can not have any child elements...
            return Collections.emptyList();
        }
    }

    private Asset mapShellToAsset(AssetAdministrationShell shell) {
        return mapIdentifiableToAssetBuilder(shell)
                .dataAddress(AasDataAddress.Builder.newInstance()
                        .baseUrl(accessUrl.toString())
                        .referenceChain(createReference(KeyTypes.ASSET_ADMINISTRATION_SHELL, shell.getId()))
                        .build())
                .build();
    }

    private Asset mapConceptDescriptionToAsset(ConceptDescription conceptDescription) {
        return mapIdentifiableToAssetBuilder(conceptDescription)
                .dataAddress(AasDataAddress.Builder.newInstance()
                        .baseUrl(accessUrl.toString())
                        .referenceChain(createReference(KeyTypes.CONCEPT_DESCRIPTION, conceptDescription.getId()))
                        .build())
                .build();
    }

    private Asset mapSubmodelToAsset(Submodel submodel) {
        var reference = createReference(KeyTypes.SUBMODEL, submodel.getId());
        List<Asset> children = new ArrayList<>();
        if (!onlySubmodels) {
            children = submodel.getSubmodelElements().stream()
                    .map(elem -> mapSubmodelElementToAsset(reference, elem))
                    .toList();
        }
        return mapIdentifiableToAssetBuilder(submodel)
                .properties(Map.of(
                        "semanticId", submodel.getSemanticId(),
                        "submodelElements", children))
                .dataAddress(AasDataAddress.Builder.newInstance()
                        .baseUrl(accessUrl.toString()).referenceChain(reference)
                        .build())
                .build();
    }

    private Reference createReference(KeyTypes type, String value) {
        return new DefaultReference.Builder()
                .keys(new DefaultKey.Builder().type(type).value(value).build())
                .build();
    }

    private Reference createReference(String value, Reference parent) {
        return new DefaultReference.Builder()
                .keys(new ArrayList<>(parent.getKeys()))
                .keys(new DefaultKey.Builder().type(KeyTypes.SUBMODEL_ELEMENT).value(value).build())
                .build();
    }

}
