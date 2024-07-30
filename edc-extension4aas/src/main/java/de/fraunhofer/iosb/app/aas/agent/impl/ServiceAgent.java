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
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.ConceptDescription;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEnvironment;
import org.eclipse.edc.spi.EdcException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import static java.lang.String.format;

/**
 * Communicating with AAS service
 */
public class ServiceAgent extends AasAgent {

    public ServiceAgent(AasDataProcessorFactory aasDataProcessorFactory) {
        super(aasDataProcessorFactory);
    }

    @Override
    public SelfDescriptionRepository.SelfDescriptionSourceType supportedType() {
        return SelfDescriptionRepository.SelfDescriptionSourceType.SERVICE;
    }

    /**
     * Returns AAS environment fetched from the AAS service.
     *
     * @param url AAS service to be updated
     * @return A map with one entry. This entry is the access url and environment of the service
     */
    @Override
    public Map<String, Environment> execute(URL url) throws IOException {
        return Map.of(url.toString(), readEnvironment(url));
    }

    /**
     * Returns the AAS environment.
     */
    private Environment readEnvironment(URL aasServiceUrl)
            throws IOException {
        URL submodelUrl;
        URL shellsUrl;
        URL conceptDescriptionsUrl;
        try {
            submodelUrl = aasServiceUrl.toURI().resolve("/api/v3.0/submodels").toURL();
            shellsUrl = aasServiceUrl.toURI().resolve("/api/v3.0/shells").toURL();
            conceptDescriptionsUrl = aasServiceUrl.toURI().resolve("/api/v3.0/concept-descriptions").toURL();
        } catch (URISyntaxException resolveUriException) {
            throw new EdcException(
                    format("Error while building URLs for reading from the AAS service at %s", aasServiceUrl),
                    resolveUriException);
        }

        return new DefaultEnvironment.Builder()
                .assetAdministrationShells(readElement(shellsUrl, AssetAdministrationShell.class).stream().toList())
                .submodels(readElement(submodelUrl, Submodel.class).stream().toList())
                .conceptDescriptions(readElement(conceptDescriptionsUrl, ConceptDescription.class).stream().toList()).build();

    }


}
