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
package de.fraunhofer.iosb.app.aas.agent.impl;

import de.fraunhofer.iosb.aas.AasDataProcessorFactory;
import de.fraunhofer.iosb.app.aas.agent.AasAgent;
import de.fraunhofer.iosb.app.pipeline.PipelineFailure;
import de.fraunhofer.iosb.app.pipeline.PipelineResult;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.ConceptDescription;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEnvironment;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

/**
 * Communicating with AAS service
 */
public class ServiceAgent extends AasAgent<Environment> {

    public static final String SUBMODELS_PATH = "%s/submodels".formatted(AAS_V3_PREFIX);
    public static final String SHELLS_PATH = "%s/shells".formatted(AAS_V3_PREFIX);
    public static final String CONCEPT_DESCRIPTIONS_PATH = "%s/concept-descriptions".formatted(AAS_V3_PREFIX);

    public ServiceAgent(AasDataProcessorFactory aasDataProcessorFactory) {
        super(aasDataProcessorFactory);
    }

    /**
     * Returns the environment of an AAS service.
     *
     * @param url The AAS service's access URL
     * @return A map with one entry. This entry is the access url and environment of the service
     */
    @Override
    public PipelineResult<Environment> apply(URL url) {
        try {
            return readEnvironment(url);
        } catch (Exception e) {
            // uncaught exception!
            return PipelineResult.failure(PipelineFailure.warning(List.of(e.getClass().getSimpleName(), e.getMessage())));
        }
    }

    private PipelineResult<Environment> readEnvironment(URL aasServiceUrl) throws IOException, URISyntaxException {
        var submodelUrl = aasServiceUrl.toURI().resolve(SUBMODELS_PATH).toURL();
        var shellsUrl = aasServiceUrl.toURI().resolve(SHELLS_PATH).toURL();
        var conceptDescriptionsUrl = aasServiceUrl.toURI().resolve(CONCEPT_DESCRIPTIONS_PATH).toURL();

        Result<List<AssetAdministrationShell>> shellsResult;
        Result<List<Submodel>> submodelsResult;
        Result<List<ConceptDescription>> conceptDescriptionsResult;
        try {
            shellsResult = readElements(shellsUrl, AssetAdministrationShell.class);
            submodelsResult = readElements(submodelUrl, Submodel.class);
            conceptDescriptionsResult = readElements(conceptDescriptionsUrl, ConceptDescription.class);

        } catch (EdcException e) {
            // If an exception was raised, produce a fatal result
            return PipelineResult.failure(PipelineFailure.fatal(List.of(e.getClass().getSimpleName())));
        }

        var environment = new DefaultEnvironment.Builder()
                .assetAdministrationShells(shellsResult.succeeded() ? shellsResult.getContent() : null)
                .submodels(submodelsResult.succeeded() ? submodelsResult.getContent() : null)
                .conceptDescriptions(conceptDescriptionsResult.succeeded() ? conceptDescriptionsResult.getContent() : null)
                .build();

        var results = List.of(shellsResult, submodelsResult, conceptDescriptionsResult);

        if (results.stream().anyMatch(AbstractResult::failed)) {
            // If any request failed, produce a warning
            return PipelineResult.recoverableFailure(environment,
                    PipelineFailure.warning(results.stream()
                            .map(AbstractResult::getFailureMessages)
                            .flatMap(List::stream)
                            .toList()));
        }

        return PipelineResult.success(environment);
    }
}