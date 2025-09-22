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
import org.eclipse.digitaltwin.aas4j.v3.model.Identifiable;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static de.fraunhofer.iosb.aas.lib.type.AasConstants.AAS_V30_NAMESPACE;
import static de.fraunhofer.iosb.app.pipeline.util.PipelineUtils.extractContents;
import static de.fraunhofer.iosb.app.pipeline.util.PipelineUtils.handleError;

/**
 * Create a mapping from an AAS environment to EDC assets. This is not a holistic transformation but rather maps key
 * elements and creates appropriate data address and assetId.
 * <p>
 * Here, we control what information about the AAS elements is stored in the AssetIndex and in the extension.
 */
public class EnvironmentToAssetMapper extends PipelineStep<Map<Service, Environment>, Collection<Service>> {

    public static final String ACCESS_POLICY_FIELD = "access-policy";
    public static final String CONTRACT_POLICY_FIELD = "contract-policy";
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

        // TODO for each selected submodel element:
        //  1. find all parents up to submodel
        //  2. Get those parents and the selected element to stay in the environment asset
        //  3. If the parent elements were not in the selection, give them policies/a marker, showing no registration shall be done to edc stores

        var submodels = handleIdentifiables(environment.getSubmodels(), service, submodelMapper);
        List<Asset> shells = List.of();
        List<Asset> conceptDescriptions = List.of();

        if (!onlySubmodelsDecision.get()) {
            shells = handleIdentifiables(environment.getAssetAdministrationShells(), service, shellMapper);
            conceptDescriptions = handleIdentifiables(environment.getConceptDescriptions(), service, conceptDescriptionMapper);
        }

        if (service.hasSelectiveRegistration() && !service.getPolicyBindings().isEmpty()) {
            var policyBindings = service.getPolicyBindings();

            submodels = filterBySelection(submodels, policyBindings);

            // TODO after fine-grained element filtering, remove this next line.
            submodels = submodels.stream().map(submodel -> submodel.toBuilder().property(AAS_V30_NAMESPACE + "Submodel/" + "submodelElements",
                    null).build()).toList();

            shells = filterBySelection(shells, policyBindings);
            conceptDescriptions = filterBySelection(conceptDescriptions, policyBindings);
        }

        // We convert data addresses this late to exploit their ReferenceChains when selecting elements to register.
        if (!useAasDataAddress.get()) {
            submodels = convertDataAddresses(submodels);
            shells = convertDataAddresses(shells);
            conceptDescriptions = convertDataAddresses(conceptDescriptions);
        }

        var environmentAsset = Asset.Builder.newInstance()
                .property(SUBMODELS, submodels)
                .property(SHELLS, shells)
                .property(CONCEPT_DESCRIPTIONS, conceptDescriptions)
                .privateProperty(PolicyBinding.class.getSimpleName(), service.getPolicyBindings())
                .build();

        return PipelineResult.success(service.with(environmentAsset));
    }

    private List<Asset> filterBySelection(List<Asset> toFilter, List<PolicyBinding> selectedElements) {
        var referredElements = selectedElements.stream()
                .map(PolicyBinding::referredElement)
                .toList();

        var policiesForSelection = selectedElements.stream().collect(Collectors.toMap(PolicyBinding::referredElement, this::createMap));

        toFilter = toFilter.stream()
                .filter(reference -> referredElements
                        .contains(((AasDataAddress) reference.getDataAddress()).getReferenceChain()))
                .toList();

        toFilter = toFilter.stream().map(asset -> asset.toBuilder()
                        .privateProperties(policiesForSelection.get(((AasDataAddress) asset.getDataAddress()).getReferenceChain()))
                        .build())
                .toList();

        return toFilter;
    }

    private HashMap<String, Object> createMap(PolicyBinding binding) {
        var map = new HashMap<String, Object>(2);
        map.put(ACCESS_POLICY_FIELD, binding.accessPolicyDefinitionId());
        map.put(CONTRACT_POLICY_FIELD, binding.contractPolicyDefinitionId());
        return map;
    }

    private List<Asset> convertDataAddresses(List<Asset> assets) {
        return assets.stream().map(asset -> asset.toBuilder()
                        .dataAddress(((AasDataAddress) asset.getDataAddress())
                                .asHttpDataAddress())
                        .build())
                .toList();
    }

    private @NotNull <I extends Identifiable> List<Asset> handleIdentifiables(Collection<I> identifiables, AasProvider provider,
                                                                              Mapper<I> identifiableHandler) {
        return identifiables.stream()
                .map(submodel -> identifiableHandler.apply(submodel, provider))
                .toList();
    }
}
