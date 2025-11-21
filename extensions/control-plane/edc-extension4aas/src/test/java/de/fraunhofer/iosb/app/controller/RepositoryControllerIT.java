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

import de.fraunhofer.iosb.app.controller.dto.RemoteAasRepositoryContextDTO;
import de.fraunhofer.iosb.app.handler.edc.EdcStoreHandler;
import de.fraunhofer.iosb.client.exception.UnauthorizedException;
import de.fraunhofer.iosb.ilt.faaast.service.dataformat.SerializationException;
import de.fraunhofer.iosb.ilt.faaast.service.model.exception.UnsupportedModifierException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.util.List;

import static de.fraunhofer.iosb.app.testutils.AasCreator.getEmptyEnvironment;
import static de.fraunhofer.iosb.app.testutils.AasCreator.getEnvironment;
import static de.fraunhofer.iosb.constants.AasConstants.EDC_SETTINGS_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


public class RepositoryControllerIT extends AbstractAasServerControllerIT<RepositoryController> {

    @Override
    protected RepositoryController getTestSubject() {
        return new RepositoryController(monitor, repository, () -> "localhost", new EdcStoreHandler(assetIndex, contractDefinitionStore));
    }


    @Test
    void test_registerRepository_emptyEnvironmentNoFailure() throws SerializationException, UnsupportedModifierException, UnauthorizedException,
            ConnectException {
        mockEmptyShellRequest();
        mockEmptySubmodelRequest();
        mockEmptyConceptDescriptionRequest();

        var result = testSubject.register(new RemoteAasRepositoryContextDTO(getUri()));

        assertEquals(result, getUri());

        var handler = repository.get(result).orElseThrow();

        var selfDescription = handler.buildSelfDescription();

        assertSelfDescription(selfDescription);
        assertTrue(selfDescription.getAssetAdministrationShells().isEmpty());
        assertTrue(selfDescription.getSubmodels().isEmpty());
        assertTrue(selfDescription.getConceptDescriptions().isEmpty());
    }


    @Test
    void test_registerRepository_filledEnvironmentAllRegistered() throws UnauthorizedException,
            ConnectException, SerializationException, UnsupportedModifierException {
        Environment environment = getEnvironment();

        mockResponse(METHOD.GET, "/shells", asPage(environment.getAssetAdministrationShells()), 200);
        mockResponse(METHOD.GET, "/submodels", asPage(environment.getSubmodels()), 200);
        mockResponse(METHOD.GET, "/concept-descriptions", asPage(environment.getConceptDescriptions()), 200);

        var uri = getUri();
        new ConfigurationController(ConfigFactory.empty(), new ConsoleMonitor()).updateConfiguration("{\"" + EDC_SETTINGS_PREFIX + ".onlySubmodels\":false}");
        var result = testSubject.register(new RemoteAasRepositoryContextDTO(uri));

        assertEquals(result, uri);

        var handler = repository.get(result).orElseThrow();

        var selfDescription = handler.buildSelfDescription();

        assertSelfDescription(selfDescription);

        assertIdentifiables(environment.getAssetAdministrationShells(), selfDescription.getAssetAdministrationShells());
        assertIdentifiables(environment.getSubmodels().stream().peek(sm -> sm.setSubmodelElements(List.of())).toList(),
                selfDescription.getSubmodels());
        assertIdentifiables(environment.getConceptDescriptions(), selfDescription.getConceptDescriptions());
    }


    @Test
    void test_registerRepository_emtpyEnvironmentNoFault() throws UnauthorizedException,
            IOException, SerializationException, UnsupportedModifierException {
        Environment environment = getEmptyEnvironment();

        mockResponse(METHOD.GET, "/environment", getEmptyEnvironment(), 200);
        mockResponse(METHOD.GET, "/shells", emptyPage(), 200);
        mockResponse(METHOD.GET, "/submodels", emptyPage(), 200);
        mockResponse(METHOD.GET, "/concept-descriptions", emptyPage(), 200);

        var base = getUri();

        var result = testSubject.register(new RemoteAasRepositoryContextDTO(base));

        assertEquals(result, getUri());

        var handler = repository.get(result).orElseThrow();

        var selfDescription = handler.buildSelfDescription();

        assertSelfDescription(selfDescription);

        assertIdentifiables(environment.getAssetAdministrationShells(), selfDescription.getAssetAdministrationShells());
        assertIdentifiables(environment.getSubmodels().stream().peek(sm -> sm.setSubmodelElements(List.of())).toList(),
                selfDescription.getSubmodels());
        assertIdentifiables(environment.getConceptDescriptions(), selfDescription.getConceptDescriptions());
    }


    @Test
    void test_registerRepository_emtpyEnvironment_shouldThrow() {
        try {
            testSubject.register(new RemoteAasRepositoryContextDTO(URI.create("https://locaIhost:65432/")));
            fail();
        }
        catch (WebApplicationException expected) {
        }
    }


    private void mockEmptyShellRequest() throws SerializationException, UnsupportedModifierException {
        mockResponse(METHOD.GET, "/shells", emptyPage(), 200);
    }


    private void mockEmptySubmodelRequest() throws SerializationException, UnsupportedModifierException {
        mockResponse(METHOD.GET, "/submodels", emptyPage(), 200);
    }


    private void mockEmptyConceptDescriptionRequest() throws SerializationException, UnsupportedModifierException {
        mockResponse(METHOD.GET, "/concept-descriptions", emptyPage(), 200);
    }

}
