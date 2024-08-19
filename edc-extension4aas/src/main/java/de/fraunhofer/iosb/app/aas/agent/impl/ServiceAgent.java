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

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

/**
 * Communicating with AAS service
 */
public class ServiceAgent extends AasAgent<Environment> {

    public static final String SUBMODELS_PATH = "/api/v3.0/submodels";
    public static final String SHELLS_PATH = "/api/v3.0/shells";
    public static final String CONCEPT_DESCRIPTIONS_PATH = "/api/v3.0/concept-descriptions";

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
            return PipelineResult.success(readEnvironment(url));
        } catch (EdcException | IOException | URISyntaxException e) {
            return PipelineResult.failure(PipelineFailure.fatal(List.of(e.getMessage())));
        }
    }

    private Environment readEnvironment(URL aasServiceUrl) throws IOException, URISyntaxException {
        var submodelUrl = aasServiceUrl.toURI().resolve(SUBMODELS_PATH).toURL();
        var shellsUrl = aasServiceUrl.toURI().resolve(SHELLS_PATH).toURL();
        var conceptDescriptionsUrl = aasServiceUrl.toURI().resolve(CONCEPT_DESCRIPTIONS_PATH).toURL();

        return new DefaultEnvironment.Builder()
                .assetAdministrationShells(readElements(shellsUrl, AssetAdministrationShell.class).stream().toList())
                .submodels(readElements(submodelUrl, Submodel.class).stream().toList())
                .conceptDescriptions(readElements(conceptDescriptionsUrl, ConceptDescription.class).stream().toList()).build();
    }
}
