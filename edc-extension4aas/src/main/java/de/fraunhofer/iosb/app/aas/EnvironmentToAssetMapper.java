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

import de.fraunhofer.iosb.aas.lib.model.AasProvider;
import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.aas.lib.model.impl.Service;
import de.fraunhofer.iosb.aas.lib.spi.AasDataAddress;
import de.fraunhofer.iosb.app.aas.mapper.AssetAdministrationShellMapper;
import de.fraunhofer.iosb.app.aas.mapper.ConceptDescriptionMapper;
import de.fraunhofer.iosb.app.aas.mapper.Mapper;
import de.fraunhofer.iosb.app.aas.mapper.SubmodelMapper;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.app.pipeline.PipelineFailure;
import de.fraunhofer.iosb.app.pipeline.PipelineResult;
import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.ConceptDescription;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static de.fraunhofer.iosb.app.pipeline.util.PipelineUtils.extractContents;
import static de.fraunhofer.iosb.app.pipeline.util.PipelineUtils.handleError;

/**
 * Create a mapping from an AAS environment to EDC assets. This is not a holistic transformation but rather maps key
 * elements and creates appropriate data address and assetId.
 * <p>
 * Here, we control what information about the AAS elements is stored in the AssetIndex and in the extension.
 */
public class EnvironmentToAssetMapper extends PipelineStep<Map<Service, Environment>, Collection<Service>> {

    private static final String CONCEPT_DESCRIPTIONS = "conceptDescriptions";
    private static final String SHELLS = "shells";
    private static final String SUBMODELS = "submodels";

    private final Supplier<Boolean> useAasDataAddress = () -> Configuration.getInstance().isUseAasDataPlane();
    private final Supplier<Boolean> onlySubmodelsDecision;
    private final Mapper<AssetAdministrationShell> shellMapper = new AssetAdministrationShellMapper();
    private final Mapper<Submodel> submodelMapper;
    private final Mapper<ConceptDescription> conceptDescriptionMapper = new ConceptDescriptionMapper();


    public EnvironmentToAssetMapper(Supplier<Boolean> onlySubmodelsDecision) {
        this.onlySubmodelsDecision = onlySubmodelsDecision;
        submodelMapper = new SubmodelMapper(onlySubmodelsDecision);
    }

    /**
     * Create a nested EDC asset from this environment structure. The top level asset is just to hold the shells,
     * submodels and concept descriptions and should not be added to the edc.
     *
     * @param environments AAS environments to transform to asset
     * @return Asset as described above
     */
    @Override
    public PipelineResult<Collection<Service>> apply(Map<Service, Environment> environments) {
        Collection<PipelineResult<Service>> results = new ArrayList<>();

        for (Map.Entry<Service, Environment> entry : environments.entrySet()) {
            results.add(
                    entry.getKey() == null ?
                            PipelineResult.failure(PipelineFailure.warning(
                                    List.of("A service in the pipeline is undefined"))) :
                            executeSingle(entry.getKey(), entry.getValue()));
        }

        var contents = extractContents(results);

        return Objects.requireNonNullElse(handleError(results, contents), PipelineResult.success(contents));
    }

    public PipelineResult<Service> executeSingle(Service service, Environment environment) {
        if (service == null || service.getAccessUrl() == null) {
            return PipelineResult.failure(PipelineFailure.fatal(List.of("Mapping failure: accessUrl is null")));
        } else if (environment == null) {
            return PipelineResult.recoverableFailure(service,
                    PipelineFailure.warning(List.of("Mapping failure for accessUrl %s: environment is null"
                            .formatted(service.getAccessUrl()))));
        }

        var submodels = handleSubmodels(environment.getSubmodels(), service);
        List<Asset> shells = List.of();
        List<Asset> conceptDescriptions = List.of();

        if (!service.hasSelectiveRegistration() && !onlySubmodelsDecision.get()) {
            shells = handleShells(environment.getAssetAdministrationShells(), service);
            conceptDescriptions = handleConceptDescriptions(environment.getConceptDescriptions(), service);
        }


        if (service.hasSelectiveRegistration()) {
            var selectedAasElements = service.getPolicyBindings().stream()
                    .map(PolicyBinding::referredElement)
                    .toList();

            submodels = filterBySelection(submodels, selectedAasElements);
            shells = filterBySelection(shells, selectedAasElements);
            conceptDescriptions = filterBySelection(conceptDescriptions, selectedAasElements);
        }

        // We convert data addresses this late to exploit their ReferenceChains when selecting elements to register.
        if (this.useAasDataAddress.get()) {
            convertDataAddresses(submodels);
            convertDataAddresses(shells);
            convertDataAddresses(conceptDescriptions);
        }

        var environmentAsset = Asset.Builder.newInstance()
                .property(SUBMODELS, submodels)
                .property(SHELLS, shells)
                .property(CONCEPT_DESCRIPTIONS, conceptDescriptions)
                .build();

        return PipelineResult.success(service.with(environmentAsset));
    }

    private List<Asset> filterBySelection(List<Asset> toFilter, List<Reference> selectedElements) {
        return toFilter.stream().filter(referable -> selectedElements
                        .contains(((AasDataAddress) referable.getDataAddress()).getReferenceChain()))
                .toList();
    }

    private void convertDataAddresses(List<Asset> assets) {
        assets.forEach(referable -> referable.toBuilder()
                .dataAddress(((AasDataAddress) referable.getDataAddress())
                        .asHttpDataAddress()));
    }

    private @NotNull List<Asset> handleSubmodels(Collection<Submodel> submodels, AasProvider provider) {
        return submodels.stream()
                .map(submodel -> submodelMapper.map(submodel, provider))
                .toList();
    }

    private @NotNull List<Asset> handleShells(Collection<AssetAdministrationShell> shells, AasProvider provider) {
        return shells.stream()
                .map(shell -> shellMapper.map(shell, provider))
                .toList();
    }

    private @NotNull List<Asset> handleConceptDescriptions(Collection<ConceptDescription> conceptDescriptions,
                                                           AasProvider provider) {
        return conceptDescriptions.stream()
                .map(conceptDescription -> conceptDescriptionMapper.map(conceptDescription, provider))
                .toList();
    }
}
