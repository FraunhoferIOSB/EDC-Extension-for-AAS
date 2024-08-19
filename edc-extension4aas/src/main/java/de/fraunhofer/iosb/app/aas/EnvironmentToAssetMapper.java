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

import de.fraunhofer.iosb.app.model.aas.AasAccessUrl;
import de.fraunhofer.iosb.app.model.aas.service.Service;
import de.fraunhofer.iosb.app.pipeline.PipelineFailure;
import de.fraunhofer.iosb.app.pipeline.PipelineResult;
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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Create a mapping from an AAS environment to EDC assets.
 * This is not a holistic transformation but rather maps some
 * key elements and creates appropriate data address and assetId.
 */
public class EnvironmentToAssetMapper extends PipelineStep<Map<AasAccessUrl, Environment>, Collection<Service>> {
    private static final String CONCEPT_DESCRIPTIONS = "conceptDescriptions";
    private static final String SHELLS = "shells";
    private static final String SUBMODELS = "submodels";
    private static final String AAS_V3_PREFIX = "/api/v3.0";
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
    public PipelineResult<Collection<Service>> apply(Map<AasAccessUrl, Environment> environments) {
        var results = environments.entrySet().stream()
                .map(entry -> executeSingle(entry.getKey().url(), entry.getValue())).toList();

        var correctResults = results.stream()
                .filter(PipelineResult::succeeded)
                .map(PipelineResult::getContent)
                .collect(Collectors.toCollection(ArrayList::new));

        if (results.stream().anyMatch(PipelineResult::failed)) {
            return PipelineResult.recoverableFailure(
                    correctResults,
                    PipelineFailure.warning(
                            results.stream()
                                    .filter(PipelineResult::failed)
                                    .map(PipelineResult::getFailureMessages)
                                    .flatMap(List::stream)
                                    .toList()));
        }

        return PipelineResult.success(correctResults);
    }

    public PipelineResult<Service> executeSingle(URL accessUrl, Environment environment) {
        if (accessUrl == null || environment == null) {
            return PipelineResult.failure(PipelineFailure.fatal(
                    List.of("Mapping failure for accessUrl %s and environment %s"
                            .formatted(accessUrl, environment))));
        }

        var assetBuilder = Asset.Builder.newInstance();
        // Add the /api/v3.0 prefix to each element since this is AAS spec
        var accessUrlV3 = accessUrl.toString().concat(AAS_V3_PREFIX);
        // Submodels
        assetBuilder.property(SUBMODELS, environment.getSubmodels().stream()
                        .map(submodel -> mapSubmodelToAsset(submodel, accessUrlV3))
                        .toList());

        if (onlySubmodelsDecision.get()) {
            assetBuilder.property(SHELLS, List.of());
            assetBuilder.property(CONCEPT_DESCRIPTIONS, List.of());
            return PipelineResult.success(new Service(accessUrl, assetBuilder.build()));
        }
        return PipelineResult.success(new Service(accessUrl,
                assetBuilder
                        .property(SHELLS,
                                environment.getAssetAdministrationShells().stream().map((AssetAdministrationShell shell) -> mapShellToAsset(shell, accessUrlV3)).toList())
                        .property(CONCEPT_DESCRIPTIONS,
                                environment.getConceptDescriptions().stream().map((ConceptDescription conceptDescription) -> mapConceptDescriptionToAsset(conceptDescription, accessUrlV3)).toList())
                        .build()));
    }

    private <R extends Referable> Asset.Builder mapReferableToAssetBuilder(R referable) {
        return Asset.Builder.newInstance()
                .properties(Map.of(
                        "idShort", Optional.ofNullable(referable.getIdShort()).orElse(""),
                        "name", referable.getDisplayName(),
                        "description", referable.getDescription()));
    }

    private <I extends Identifiable> Asset.Builder mapIdentifiableToAssetBuilder(I identifiable) {
        var admin = Optional.ofNullable(identifiable.getAdministration())
                .orElse(new DefaultAdministrativeInformation.Builder().build());
        var version = null != admin.getVersion() && null != admin.getRevision() ?
                String.valueOf(admin.getVersion()).concat(":").concat(String.valueOf(admin.getRevision())) : null;

        return mapReferableToAssetBuilder(identifiable)
                .version(version)
                .contentType("application/json")
                .properties(Map.of(
                        "id", identifiable.getId(),
                        "embeddedDataSpecifications", admin.getEmbeddedDataSpecifications()));
    }

    /* May contain traces of recursion */
    private <E extends SubmodelElement> Asset mapSubmodelElementToAsset(Reference parentReference, E submodelElement,
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
                .id(String.valueOf("%s:%s".formatted(accessUrl, dataAddress.referenceChainAsPath()).hashCode()))
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
        var dataAddress = AasDataAddress.Builder.newInstance()
                .baseUrl(accessUrl)
                .referenceChain(createReference(KeyTypes.ASSET_ADMINISTRATION_SHELL, shell.getId()))
                .build();

        return mapIdentifiableToAssetBuilder(shell)
                .id(String.valueOf("%s:%s".formatted(accessUrl, dataAddress.referenceChainAsPath()).hashCode()))
                .dataAddress(dataAddress)
                .build();
    }

    private Asset mapConceptDescriptionToAsset(ConceptDescription conceptDescription, String accessUrl) {
        var dataAddress = AasDataAddress.Builder.newInstance()
                .baseUrl(accessUrl)
                .referenceChain(createReference(KeyTypes.CONCEPT_DESCRIPTION, conceptDescription.getId()))
                .build();

        return mapIdentifiableToAssetBuilder(conceptDescription)
                .id(String.valueOf("%s:%s".formatted(accessUrl, dataAddress.referenceChainAsPath()).hashCode()))
                .dataAddress(dataAddress)
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

        var dataAddress = AasDataAddress.Builder.newInstance()
                .baseUrl(accessUrl).referenceChain(reference)
                .build();

        return mapIdentifiableToAssetBuilder(submodel)
                .id(String.valueOf("%s:%s".formatted(accessUrl, dataAddress.referenceChainAsPath()).hashCode()))
                .properties(Map.of(
                        "semanticId",
                        Optional.ofNullable(submodel.getSemanticId())
                                .orElse(new DefaultReference.Builder().build()),
                        "submodelElements", children))
                .dataAddress(dataAddress)
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
