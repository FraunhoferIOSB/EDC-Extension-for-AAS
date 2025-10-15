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
package de.fraunhofer.iosb.app.aas.mapper.environment;

import de.fraunhofer.iosb.aas.lib.model.AasProvider;
import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.aas.lib.model.impl.Service;
import de.fraunhofer.iosb.aas.lib.spi.AasDataAddress;
import de.fraunhofer.iosb.app.aas.mapper.environment.referable.identifiable.IdentifiableMapper;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.app.pipeline.PipelineFailure;
import de.fraunhofer.iosb.app.pipeline.PipelineResult;
import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import de.fraunhofer.iosb.app.util.AssetUtil;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Identifiable;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static de.fraunhofer.iosb.app.aas.mapper.environment.referable.identifiable.IdentifiableMapper.SUBMODEL_ELEMENT_LOCATION;
import static de.fraunhofer.iosb.app.edc.contract.ContractRegistrar.DEFAULT_ACCESS_POLICY_DEFINITION_ID;
import static de.fraunhofer.iosb.app.edc.contract.ContractRegistrar.DEFAULT_CONTRACT_POLICY_DEFINITION_ID;
import static de.fraunhofer.iosb.app.pipeline.util.PipelineUtils.extractContents;
import static de.fraunhofer.iosb.app.pipeline.util.PipelineUtils.handleError;
import static de.fraunhofer.iosb.app.util.AssetUtil.applyRecursive;

/**
 * Create a mapping from an AAS environment to EDC assets. This is not a holistic transformation but rather maps key
 * elements and creates appropriate data address and assetId.
 * <p>
 * Here, we control what information about the AAS elements is stored in the AssetIndex and in the extension.
 */
public class EnvironmentToAssetMapper extends PipelineStep<Map<Service, Environment>, Collection<Service>> {

    public static final String ACCESS_POLICY_FIELD = "accessPolicyId";
    public static final String CONTRACT_POLICY_FIELD = "contractPolicyId";
    public static final String CONCEPT_DESCRIPTIONS_LOCATION = "conceptDescriptions";
    public static final String SHELLS_LOCATION = "shells";
    public static final String SUBMODELS_LOCATION = "submodels";

    private final Supplier<Boolean> useAasDataAddress = () -> Configuration.getInstance().useAasDataPlane();
    private final Supplier<Boolean> onlySubmodelsDecision = () -> Configuration.getInstance().onlySubmodels();
    private final IdentifiableMapper identifiableMapper = new IdentifiableMapper();


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
        if (service == null || service.baseUrl() == null) {
            return PipelineResult.failure(PipelineFailure.fatal(List.of("Mapping failure: accessUrl is null")));
        } else if (environment == null) {
            return PipelineResult.recoverableFailure(service,
                    PipelineFailure.warning(List.of("Mapping failure for accessUrl %s: environment is null"
                            .formatted(service.baseUrl()))));
        }

        var submodels = mapIdentifiableList(environment.getSubmodels(), service);
        List<Asset> shells = List.of();
        List<Asset> conceptDescriptions = List.of();

        if (!onlySubmodelsDecision.get()) {
            shells = mapIdentifiableList(environment.getAssetAdministrationShells(), service);
            conceptDescriptions = mapIdentifiableList(environment.getConceptDescriptions(), service);
        }

        if (service.hasSelectiveRegistration()) {
            var policyBindings = service.getPolicyBindings();

            mapPolicies(submodels, policyBindings);
            submodels.stream().map(AssetUtil::flatMapAssets).flatMap(Collection::stream)
                    .forEach(asset -> mapPolicies(asset, policyBindings));

            mapPolicies(shells, policyBindings);
            mapPolicies(conceptDescriptions, policyBindings);
        } else {
            mapPolicies(submodels);
            submodels.forEach(submodel -> AssetUtil.getChildren(submodel, SUBMODEL_ELEMENT_LOCATION)
                    .forEach(submodelElement -> applyRecursive(submodelElement, this::mapPolicies)));
            mapPolicies(shells);
            mapPolicies(conceptDescriptions);
        }

        // We convert data addresses this late to exploit their ReferenceChains when selecting elements to register.
        if (!useAasDataAddress.get()) {
            submodels = submodels.stream().map(this::convertDataAddress).toList();
            for (var submodel : submodels) {
                var smes = AssetUtil.getChildren(submodel, SUBMODEL_ELEMENT_LOCATION);
                smes = smes.stream().map(sme -> applyRecursive(sme, this::convertDataAddress)).toList();
                submodel.getProperties().put(SUBMODEL_ELEMENT_LOCATION, smes);
            }
            shells = shells.stream().map(this::convertDataAddress).toList();
            conceptDescriptions = conceptDescriptions.stream().map(this::convertDataAddress).toList();
        }

        var environmentAsset = Asset.Builder.newInstance()
                .property(SUBMODELS_LOCATION, submodels)
                .property(SHELLS_LOCATION, shells)
                .property(CONCEPT_DESCRIPTIONS_LOCATION, conceptDescriptions)
                .privateProperty(PolicyBinding.class.getSimpleName(), service.getPolicyBindings())
                .build();

        return PipelineResult.success(service.with(environmentAsset));
    }

    private void mapPolicies(List<Asset> toMap, List<PolicyBinding> selectedElements) {
        toMap.forEach(asset -> mapPolicies(asset, selectedElements));
    }

    private void mapPolicies(Asset toMap, List<PolicyBinding> selectedElements) {
        Map<Reference, Map<String, Object>> policyLookupTable = selectedElements.stream().collect(Collectors.toMap(PolicyBinding::referredElement,
                this::createPolicyMap));

        toMap.getPrivateProperties().putAll(policyLookupTable.get(((AasDataAddress) toMap.getDataAddress()).getReferenceChain()));
    }

    private void mapPolicies(List<Asset> toMap) {
        toMap.forEach(this::mapPolicies);
    }

    private Asset mapPolicies(Asset toMap) {
        Map<String, Object> defaultPolicies = Map.of(ACCESS_POLICY_FIELD, DEFAULT_ACCESS_POLICY_DEFINITION_ID,
                CONTRACT_POLICY_FIELD, DEFAULT_CONTRACT_POLICY_DEFINITION_ID);

        toMap.getPrivateProperties().putAll(defaultPolicies);

        return toMap;
    }

    private Map<String, Object> createPolicyMap(PolicyBinding binding) {
        String accessPolicyId =
                Optional.ofNullable(binding.accessPolicyDefinitionId())
                        .orElse(DEFAULT_ACCESS_POLICY_DEFINITION_ID);

        String contractPolicyId =
                Optional.ofNullable(binding.contractPolicyDefinitionId())
                        .orElse(DEFAULT_CONTRACT_POLICY_DEFINITION_ID);

        return Map.of(ACCESS_POLICY_FIELD, accessPolicyId, CONTRACT_POLICY_FIELD, contractPolicyId);
    }

    private Asset convertDataAddress(Asset asset) {
        return asset.toBuilder()
                .dataAddress(((AasDataAddress) asset.getDataAddress())
                        .asHttpDataAddress())
                .build();
    }

    private @NotNull <I extends Identifiable> List<Asset> mapIdentifiableList(Collection<I> identifiableList, AasProvider provider) {
        return identifiableList.stream()
                .map(identifiable -> identifiableMapper.map(identifiable, provider))
                .toList();
    }
}
