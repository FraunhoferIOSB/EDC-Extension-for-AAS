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
package de.fraunhofer.iosb.app.aas;

import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.ConceptDescription;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Identifiable;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.Referable;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementCollection;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementList;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAdministrativeInformation;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultKey;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Create a mapping from an AAS environment to EDC assets.
 * This is not a holistic transformation but rather maps some
 * key elements and creates appropriate data address and assetId.
 * TODO Putting env -> map -> asset on ice for now
 * TODO accessUrl as argument is not good
 */
public class EnvironmentToAssetMapper extends PipelineStep<Map<String, Environment>, Map<String, Asset>> {
    private final boolean onlySubmodels;

    public EnvironmentToAssetMapper(boolean onlySubmodels) {
        this.onlySubmodels = onlySubmodels;
    }

    @Deprecated(since = "Replacing with pipeline")
    public Asset map(String accessUrl, Environment environment) {
        return executeSingle(accessUrl, environment).getValue();
    }

    /**
     * Create a nested EDC asset from this environment structure. The top level asset is just to hold the shells,
     * submodels and concept descriptions and should not be added to assetIndex.
     *
     * @param environments AAS environments to transform to asset
     * @return Asset as described above
     */
    @Override
    public Map<String, Asset> execute(Map<String, Environment> environments) {
        return environments.entrySet().stream()
                .map(entry -> executeSingle(entry.getKey(), entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map.Entry<String, Asset> executeSingle(String accessUrl, Environment environment) {
        var assetBuilder = Asset.Builder.newInstance().property("submodels",
                environment.getSubmodels().stream().map((Submodel submodel) -> mapSubmodelToAsset(submodel, accessUrl)).toList());
        if (onlySubmodels) {
            return new AbstractMap.SimpleEntry<>(accessUrl, assetBuilder.build());
        }
        return new AbstractMap.SimpleEntry<>(accessUrl,
                assetBuilder
                        .property("shells",
                                environment.getAssetAdministrationShells().stream().map((AssetAdministrationShell shell) -> mapShellToAsset(shell, accessUrl)).toList())
                        .property("conceptDescriptions",
                                environment.getConceptDescriptions().stream().map((ConceptDescription conceptDescription) -> mapConceptDescriptionToAsset(conceptDescription, accessUrl)).toList())
                        .build());
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
    private <T extends SubmodelElement> Asset mapSubmodelElementToAsset(Reference parentReference, T submodelElement, String accessUrl) {
        var reference = createReference(submodelElement.getIdShort(), parentReference);

        var children = getContainerElements(submodelElement).stream()
                .map(elem -> mapSubmodelElementToAsset(reference, elem, accessUrl))
                .toList();

        return mapReferableToAssetBuilder(submodelElement)
                .id(String.valueOf(submodelElement.getIdShort().hashCode()))
                .contentType("application/json")
                .properties(Map.of(
                        "embeddedDataSpecifications", submodelElement.getEmbeddedDataSpecifications(),
                        "semanticId", submodelElement.getSemanticId(),
                        "value", children))
                .dataAddress(AasDataAddress.Builder.newInstance()
                        .baseUrl(accessUrl)
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

    private Asset mapShellToAsset(AssetAdministrationShell shell, String accessUrl) {
        return mapIdentifiableToAssetBuilder(shell)
                .dataAddress(AasDataAddress.Builder.newInstance()
                        .baseUrl(accessUrl)
                        .referenceChain(createReference(KeyTypes.ASSET_ADMINISTRATION_SHELL, shell.getId()))
                        .build())
                .build();
    }

    private Asset mapConceptDescriptionToAsset(ConceptDescription conceptDescription, String accessUrl) {
        return mapIdentifiableToAssetBuilder(conceptDescription)
                .dataAddress(AasDataAddress.Builder.newInstance()
                        .baseUrl(accessUrl)
                        .referenceChain(createReference(KeyTypes.CONCEPT_DESCRIPTION, conceptDescription.getId()))
                        .build())
                .build();
    }

    private Asset mapSubmodelToAsset(Submodel submodel, String accessUrl) {
        var reference = createReference(KeyTypes.SUBMODEL, submodel.getId());
        List<Asset> children = new ArrayList<>();
        if (!onlySubmodels) {
            children = submodel.getSubmodelElements().stream()
                    .map(elem -> mapSubmodelElementToAsset(reference, elem, accessUrl))
                    .toList();
        }
        return mapIdentifiableToAssetBuilder(submodel)
                .properties(Map.of(
                        "semanticId", submodel.getSemanticId(),
                        "submodelElements", children))
                .dataAddress(AasDataAddress.Builder.newInstance()
                        .baseUrl(accessUrl).referenceChain(reference)
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
