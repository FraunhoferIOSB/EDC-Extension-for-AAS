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

import de.fraunhofer.iosb.app.aas.mapper.ConceptDescriptionMapper;
import de.fraunhofer.iosb.app.aas.mapper.ShellToAssetMapper;
import de.fraunhofer.iosb.app.aas.mapper.SubmodelMapper;
import de.fraunhofer.iosb.app.model.aas.service.Service;
import de.fraunhofer.iosb.app.pipeline.PipelineFailure;
import de.fraunhofer.iosb.app.pipeline.PipelineResult;
import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.ConceptDescription;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import java.net.MalformedURLException;
import java.net.URL;
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

    private final Supplier<Boolean> onlySubmodelsDecision;
    private final ShellToAssetMapper shellMapper = new ShellToAssetMapper();
    private final SubmodelMapper submodelMapper;
    private final ConceptDescriptionMapper conceptDescriptionMapper = new ConceptDescriptionMapper();


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
            var service = Objects.requireNonNullElse(entry.getKey(), new Service((URL) null));
            results.add(executeSingle(service, entry.getValue()));
        }

        var contents = extractContents(results);

        return Objects.requireNonNullElse(handleError(results, contents), PipelineResult.success(contents));
    }

    public PipelineResult<Service> executeSingle(Service service, Environment environment) {
        if (service == null || service.getAccessUrl() == null) {
            return PipelineResult.failure(PipelineFailure.fatal(
                    List.of("Mapping failure: accessUrl is null")));
        } else if (environment == null) {
            return PipelineResult.recoverableFailure(service,
                    PipelineFailure.warning(List.of("Mapping failure for accessUrl %s: environment is null"
                            .formatted(service.getAccessUrl()))));
        }

        var assetBuilder = Asset.Builder.newInstance();

        try {
            final String submodelsUrl = service.getSubmodelsUrl().toString();
            assetBuilder.property(SUBMODELS, environment.getSubmodels().stream()
                    .map(submodel -> submodelMapper.map(submodel, submodelsUrl))
                    .toList());
        } catch (MalformedURLException e) {
            return PipelineResult.failure(PipelineFailure.warning(List.of("Could not build access url for %s".formatted(service.getAccessUrl()))));
        }

        if (onlySubmodelsDecision.get()) {
            assetBuilder.property(SHELLS, List.of());
            assetBuilder.property(CONCEPT_DESCRIPTIONS, List.of());
            return PipelineResult.success(service.with(assetBuilder.build()));
        }

        try {
            var shellsUrl = service.getShellsUrl().toString();
            var conceptDescriptionsUrl = service.getConceptDescriptionsUrl().toString();

            return PipelineResult.success(service.with(
                    assetBuilder
                            .property(SHELLS,
                                    environment.getAssetAdministrationShells().stream()
                                            .map((AssetAdministrationShell shell) -> shellMapper.map(shell, shellsUrl))
                                            .toList())
                            .property(CONCEPT_DESCRIPTIONS,
                                    environment.getConceptDescriptions().stream()
                                            .map((ConceptDescription conceptDescription) -> conceptDescriptionMapper.map(conceptDescription, conceptDescriptionsUrl))
                                            .toList())
                            .build()));
        } catch (MalformedURLException e) {
            return PipelineResult.recoverableFailure(service.with(assetBuilder.build()),
                    PipelineFailure.warning(List.of("Could not build access url for %s".formatted(service.getAccessUrl()))));
        }
    }
}
