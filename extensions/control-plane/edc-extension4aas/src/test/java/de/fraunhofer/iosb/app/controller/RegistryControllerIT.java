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

import de.fraunhofer.iosb.app.controller.dto.AasRegistryContextDTO;
import de.fraunhofer.iosb.app.handler.aas.registry.RemoteAasRegistryHandler;
import de.fraunhofer.iosb.app.handler.edc.EdcStoreHandler;
import de.fraunhofer.iosb.app.testutils.MockServerTestExtension;
import de.fraunhofer.iosb.client.exception.UnauthorizedException;
import de.fraunhofer.iosb.ilt.faaast.service.dataformat.SerializationException;
import de.fraunhofer.iosb.ilt.faaast.service.model.exception.UnsupportedModifierException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShellDescriptor;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static de.fraunhofer.iosb.app.testutils.RegistryElementCreator.asShell;
import static de.fraunhofer.iosb.app.testutils.RegistryElementCreator.asSubmodel;
import static de.fraunhofer.iosb.app.testutils.RegistryElementCreator.getShellDescriptor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


public class RegistryControllerIT extends AbstractAasServerControllerIT<RegistryController> {

    @Override
    protected RegistryController getTestSubject() {
        return new RegistryController(monitor, aasServerStore, new EdcStoreHandler(assetIndex, contractDefinitionStore));
    }


    @Test
    void test_register_singleShellDescriptorIncludingASubmodelDescriptor()
            throws UnauthorizedException, ConnectException, UnsupportedModifierException, de.fraunhofer.iosb.ilt.faaast.service.dataformat.SerializationException {
        AssetAdministrationShellDescriptor shellDescriptor = getShellDescriptor();

        mockResponse(MockServerTestExtension.METHOD.GET, String.format("/%s", "shell-descriptors"), asPage(List.of(shellDescriptor)), 200);
        mockEmptySubmodelDescriptorRequest();

        var result = testSubject.register(new AasRegistryContextDTO(getUri()));

        assertEquals(result, getUri());

        var handler = (RemoteAasRegistryHandler) aasServerStore.get(result).orElseThrow();

        var selfDescription = handler.buildSelfDescription();

        assertSelfDescription(selfDescription);
        assertFalse(selfDescription.getAssetAdministrationShells().isEmpty());
        assertFalse(selfDescription.getSubmodels().isEmpty());
        assertTrue(selfDescription.getConceptDescriptions().isEmpty());

        assertIdentifiables(List.of(asShell(shellDescriptor)), selfDescription.getAssetAdministrationShells());
        assertIdentifiables(List.of(asSubmodel(shellDescriptor.getSubmodelDescriptors().get(0))), selfDescription.getSubmodels());
    }


    @Test
    void test_register_emptyDescriptorResponseNoFault() throws SerializationException, UnsupportedModifierException, UnauthorizedException, ConnectException {
        mockEmptyShellDescriptorRequest();
        mockEmptySubmodelDescriptorRequest();

        var result = testSubject.register(new AasRegistryContextDTO(getUri()));

        assertEquals(result, getUri());

        var handler = (RemoteAasRegistryHandler) aasServerStore.get(result).orElseThrow();

        var selfDescription = handler.buildSelfDescription();

        assertSelfDescription(selfDescription);
        assertTrue(selfDescription.getAssetAdministrationShells().isEmpty());
        assertTrue(selfDescription.getSubmodels().isEmpty());
        assertTrue(selfDescription.getConceptDescriptions().isEmpty());
    }


    @Test
    void test_register_notARegistryFailsRegistration() {
        URI uri;
        try {
            uri = new URI("https://example.com");
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        try {
            testSubject.register(new AasRegistryContextDTO(uri));
            fail();
        }
        catch (WebApplicationException expected) {
        }
    }


    private void mockEmptyShellDescriptorRequest() throws SerializationException, UnsupportedModifierException {
        mockResponse(METHOD.GET, String.format("/%s", "shell-descriptors"), emptyPage(), 200);
    }


    private void mockEmptySubmodelDescriptorRequest() throws de.fraunhofer.iosb.ilt.faaast.service.dataformat.SerializationException, UnsupportedModifierException {
        mockResponse(METHOD.GET, String.format("/%s", "submodel-descriptors"), emptyPage(), 200);
    }

}
