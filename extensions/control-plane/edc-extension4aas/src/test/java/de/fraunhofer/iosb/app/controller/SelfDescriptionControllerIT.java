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
package de.fraunhofer.iosb.app.controller;

import de.fraunhofer.iosb.app.handler.aas.repository.AasRepositoryHandler;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.client.exception.UnauthorizedException;
import de.fraunhofer.iosb.ilt.faaast.service.dataformat.SerializationException;
import de.fraunhofer.iosb.ilt.faaast.service.model.exception.UnsupportedModifierException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;

import static de.fraunhofer.iosb.app.testutils.AasCreator.getEnvironment;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class SelfDescriptionControllerIT extends AbstractAasServerControllerIT<SelfDescriptionController> {

    @Test
    public void getSelfDescription_noExposedFields_shouldNotBeEmpty()
            throws SerializationException, UnsupportedModifierException, UnauthorizedException, ConnectException, DeserializationException {
        Environment environment = getEnvironment();

        mockResponse(METHOD.GET, "/shells", asPage(environment.getAssetAdministrationShells()), 200);
        mockResponse(METHOD.GET, "/submodels", asPage(environment.getSubmodels()), 200);
        mockResponse(METHOD.GET, "/concept-descriptions", asPage(environment.getConceptDescriptions()), 200);

        var uri = getUri();

        Configuration.getInstance().setExposedFields("");
        var mockHandler = mock(AasRepositoryHandler.class);

        when(mockHandler.buildSelfDescription()).thenReturn(environment);
        aasServerStore.put(uri, mockHandler);
        var selfDescription = testSubject.getSelfDescription(uri);
        var selfDescriptionsDeserialized = new JsonDeserializer().readList(selfDescription, Environment.class);

        Environment selfDescriptionSerialized = selfDescriptionsDeserialized.get(0);

        assertSelfDescription(selfDescriptionSerialized);

        assertEquals(selfDescriptionSerialized.getAssetAdministrationShells().size(), environment.getAssetAdministrationShells().size());
        assertEquals(selfDescriptionSerialized.getSubmodels().size(), environment.getSubmodels().size());

        for (int i = 0; i < selfDescriptionSerialized.getSubmodels().size(); i++) {
            assertEquals(selfDescriptionSerialized.getSubmodels().get(i).getSubmodelElements().size(),
                    environment.getSubmodels().get(i).getSubmodelElements().size());
        }

        assertEquals(selfDescriptionSerialized.getConceptDescriptions().size(), environment.getConceptDescriptions().size());
    }


    @Test
    public void getSelfDescription_specificExposedField_shouldContainField()
            throws SerializationException, UnsupportedModifierException, UnauthorizedException, ConnectException, DeserializationException {
        Environment environment = getEnvironment();

        mockResponse(METHOD.GET, "/shells", asPage(environment.getAssetAdministrationShells()), 200);
        mockResponse(METHOD.GET, "/submodels", asPage(environment.getSubmodels()), 200);
        mockResponse(METHOD.GET, "/concept-descriptions", asPage(environment.getConceptDescriptions()), 200);

        var uri = getUri();

        // Add necessary fields for semantic id
        Configuration.getInstance().setExposedFields("HasSemantics/semanticId,Reference/keys,Reference/type,Key/type,Key/value");
        var mockHandler = mock(AasRepositoryHandler.class);

        when(mockHandler.buildSelfDescription()).thenReturn(environment);
        aasServerStore.put(uri, mockHandler);
        var selfDescription = testSubject.getSelfDescription(uri);
        var selfDescriptionsDeserialized = new JsonDeserializer().readList(selfDescription, Environment.class);

        Environment selfDescriptionSerialized = selfDescriptionsDeserialized.get(0);

        assertSelfDescription(selfDescriptionSerialized);

        assertEquals(selfDescriptionSerialized.getAssetAdministrationShells().size(), environment.getAssetAdministrationShells().size());
        assertEquals(selfDescriptionSerialized.getSubmodels().size(), environment.getSubmodels().size());

        for (int i = 0; i < selfDescriptionSerialized.getSubmodels().size(); i++) {
            assertEquals(selfDescriptionSerialized.getSubmodels().get(i).getSubmodelElements().size(),
                    environment.getSubmodels().get(i).getSubmodelElements().size());

            assertEquals(selfDescriptionSerialized.getSubmodels().get(i).getSemanticId(),
                    environment.getSubmodels().get(i).getSemanticId());
        }

        assertEquals(selfDescriptionSerialized.getConceptDescriptions().size(), environment.getConceptDescriptions().size());
    }


    @Override
    protected SelfDescriptionController getTestSubject() {
        return new SelfDescriptionController(monitor, aasServerStore);
    }
}
