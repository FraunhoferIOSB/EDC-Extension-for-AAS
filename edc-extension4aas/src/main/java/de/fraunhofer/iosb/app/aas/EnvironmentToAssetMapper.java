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

import de.fraunhofer.iosb.app.pipeline.PipelineFailure;
import de.fraunhofer.iosb.app.pipeline.PipelineResult;
import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress;
import org.eclipse.digitaltwin.aas4j.v3.model.*;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAdministrativeInformation;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultKey;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Create a mapping from an AAS environment to EDC assets.
 * This is not a holistic transformation but rather maps some
 * key elements and creates appropriate data address and assetId.
 * TODO Putting env -> map -> asset on ice for now
 */
public class EnvironmentToAssetMapper extends PipelineStep<Map<String, Environment>, Map<String, Asset>> {
    private static final String CONCEPT_DESCRIPTIONS = "conceptDescriptions";
    private static final String SHELLS = "shells";
    private static final String SUBMODELS = "submodels";
    private final Supplier<Boolean> onlySubmodelsDecision;

    public EnvironmentToAssetMapper(Supplier<Boolean> onlySubmodelsDecision) {
        this.onlySubmodelsDecision = onlySubmodelsDecision;
    }

    /**
     * Create a nested EDC asset from this environment structure. The top level asset is just to hold the shells,
     * submodels and concept descriptions and should not be added to the edc.
     *
     * @param environments AAS environments to transform to asset
     * @return Asset as described above
     */
    @Override
    public PipelineResult<Map<String, Asset>> apply(Map<String, Environment> environments) {
        var results = environments.entrySet().stream()
                .map(entry -> executeSingle(entry.getKey(), entry.getValue())).toList();

        if (results.stream().anyMatch(PipelineResult::failed)) {
            return PipelineResult.negligibleFailure(
                    results.stream()
                            .filter(PipelineResult::succeeded)
                            .map(PipelineResult::getContent)
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                    PipelineFailure.warning(
                            results.stream()
                                    .filter(PipelineResult::failed)
                                    .map(PipelineResult::getFailureMessages)
                                    .flatMap(List::stream)
                                    .toList()));
        }

        return PipelineResult.success(
                results.stream()
                        .map(PipelineResult::getContent)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    public PipelineResult<Map.Entry<String, Asset>> executeSingle(String accessUrl, Environment environment) {
        if (accessUrl == null || environment == null) {
            return PipelineResult.failure(PipelineFailure.fatal(
                    List.of("Mapping failure for accessUrl %s and environment %s"
                            .formatted(accessUrl, environment))));
        }

        var assetBuilder = Asset.Builder.newInstance().property(SUBMODELS,
                environment.getSubmodels().stream().map((Submodel submodel) -> mapSubmodelToAsset(submodel,
                        accessUrl)).toList());

        if (onlySubmodelsDecision.get()) {
            assetBuilder.property(SHELLS, List.of());
            assetBuilder.property(CONCEPT_DESCRIPTIONS, List.of());
            return PipelineResult.success(new AbstractMap.SimpleEntry<>(accessUrl, assetBuilder.build()));
        }
        return PipelineResult.success(new AbstractMap.SimpleEntry<>(accessUrl,
                assetBuilder
                        .property(SHELLS,
                                environment.getAssetAdministrationShells().stream().map((AssetAdministrationShell shell) -> mapShellToAsset(shell, accessUrl)).toList())
                        .property(CONCEPT_DESCRIPTIONS,
                                environment.getConceptDescriptions().stream().map((ConceptDescription conceptDescription) -> mapConceptDescriptionToAsset(conceptDescription, accessUrl)).toList())
                        .build()));
    }

    private <T extends Referable> Asset.Builder mapReferableToAssetBuilder(T referable) {
        return Asset.Builder.newInstance()
                .properties(Map.of(
                        "idShort", Optional.ofNullable(referable.getIdShort()).orElse(""),
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
    private <T extends SubmodelElement> Asset mapSubmodelElementToAsset(Reference parentReference, T submodelElement,
                                                                        String accessUrl) {
        var reference = createReference(submodelElement.getIdShort(), parentReference);

        var children = getContainerElements(submodelElement).stream()
                .map(elem -> mapSubmodelElementToAsset(reference, elem, accessUrl))
                .toList();

        var dataAddress = AasDataAddress.Builder.newInstance()
                .baseUrl(accessUrl)
                .referenceChain(reference)
                .build();

        return mapReferableToAssetBuilder(submodelElement)
                .id(String.valueOf(dataAddress.referenceChainAsPath().hashCode()))
                .contentType("application/json")
                .properties(Map.of(
                        "embeddedDataSpecifications", submodelElement.getEmbeddedDataSpecifications(),
                        "semanticId", submodelElement.getSemanticId(),
                        "value", children))
                .dataAddress(dataAddress)
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
        if (!onlySubmodelsDecision.get()) {
            children = submodel.getSubmodelElements().stream()
                    .map(elem -> mapSubmodelElementToAsset(reference, elem, accessUrl))
                    .toList();
        }
        return mapIdentifiableToAssetBuilder(submodel)
                .properties(Map.of(
                        "semanticId",
                        Optional.ofNullable(submodel.getSemanticId())
                                .orElse(new DefaultReference.Builder().build()),
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
