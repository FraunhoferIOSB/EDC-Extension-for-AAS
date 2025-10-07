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

import de.fraunhofer.iosb.aas.lib.model.impl.Service;
import de.fraunhofer.iosb.aas.lib.spi.AasDataAddress;
import de.fraunhofer.iosb.app.aas.mapper.environment.EnvironmentToAssetMapper;
import de.fraunhofer.iosb.app.controller.ConfigurationController;
import de.fraunhofer.iosb.app.pipeline.PipelineFailure;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEnvironment;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.fraunhofer.iosb.app.testutils.AasCreator.getEmptyEnvironment;
import static de.fraunhofer.iosb.app.testutils.AasCreator.getEnvironment;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Do not test for empty Shell, Submodel, ConceptDescription inside an environment since the default implementation
 * of the environment class does not allow null values.
 */
class EnvironmentToAssetMapperTest {

    public static final String CONCEPT_DESCRIPTIONS = "conceptDescriptions";
    public static final String SHELLS = "shells";
    public static final String SUBMODELS = "submodels";
    private final URL accessUrl = new URL("http://localhost:%s".formatted(12345));
    private final List<Object> emptyList = List.of();
    private EnvironmentToAssetMapper testSubject;
    // Change for test case if needed
    private boolean onlySubmodelsDecider;

    EnvironmentToAssetMapperTest() throws MalformedURLException {
    }

    @BeforeEach
    void setUp() {
        onlySubmodelsDecider = false;

        testSubject = new EnvironmentToAssetMapper(() -> this.onlySubmodelsDecider);
    }

    @Test
    void testApplyNullEnvironment() throws MalformedURLException {
        var environment = getEnvironment();
        var realEnvironmentAccessUrl = new URL("https://example.com");

        var input = new HashMap<Service, Environment>();
        input.put(new Service.Builder().withUrl(realEnvironmentAccessUrl).build(), environment);
        input.put(new Service.Builder().withUrl(accessUrl).build(), null);

        var result = testSubject.apply(input);

        // Since there are null keys / null values, the pipeline should halt.
        assertTrue(result.failed());
        assertEquals(PipelineFailure.Type.WARNING, result.getFailure().getFailureType());
        assertNotNull(result.getContent().stream()
                .filter(service -> service.baseUrl().toString()
                        .equals(realEnvironmentAccessUrl.toString())).findFirst().orElseThrow().getEnvironment());

        assertNull(result.getContent().stream()
                .filter(service -> service.baseUrl().toString().equals(accessUrl.toString()))
                .findFirst().orElse(new Service.Builder().withUrl(new URL("http://localhost")).build())
                .getEnvironment());
    }

    @Test
    void testApplyFaultyInput() throws MalformedURLException {
        var environment = getEnvironment();
        var emptyEnvironment = getEmptyEnvironment();

        var input = new HashMap<>(Map.of(new Service.Builder().withUrl(accessUrl).build(), environment,
                new Service.Builder().withUrl(new URL("http://localhost:8080")).build(), emptyEnvironment));
        input.put(new Service.Builder().withUrl(accessUrl).build(), null);
        input.put(null, environment);

        var result = testSubject.apply(input);

        // Since there are null keys / null values, the pipeline should warn.
        // Halting the pipeline should only happen if no other services can be "serviced"
        assertTrue(result.failed());
        assertEquals(PipelineFailure.Type.WARNING, result.getFailure().getFailureType());
    }

    @Test
    void testApply() {
        var env = getEnvironment();
        var result = testSubject.apply(Map.of(new Service.Builder().withUrl(accessUrl).build(), env));
        assertTrue(result.succeeded());
        assertNotNull(result.getContent());
        var envAsset = result.getContent().stream()
                .filter(service -> service
                        .baseUrl().toString()
                        .equals(accessUrl.toString()))
                .map(Service::getEnvironment)
                .findFirst()
                .orElseThrow();

        assertNotNull(envAsset);

        // We don't compare IDs since they simply need to be unique
        assertEquals(env.getSubmodels().size(), getChildren(envAsset, SUBMODELS).size());

        assertEquals(env.getAssetAdministrationShells().size(), getChildren(envAsset, SHELLS).size());

        assertEquals(env.getConceptDescriptions().size(), getChildren(envAsset, CONCEPT_DESCRIPTIONS).size());
    }

    @Test
    void testNullAccessUrl() {
        // This can happen since map implementations can have null keys
        var res = testSubject.executeSingle(null, new DefaultEnvironment());
        assertTrue(res.failed());
    }

    @Test
    void testNullEnvironment() {
        var res = testSubject.executeSingle(new Service.Builder().withUrl(accessUrl).build(), null);
        assertTrue(res.failed());
    }

    @Test
    void testNullArgs() {
        var res = testSubject.executeSingle(null, null);
        assertTrue(res.failed());
    }

    @Test
    void testEmptyEnvironment() {
        var result = testSubject.executeSingle(new Service.Builder().withUrl(accessUrl).build(), new DefaultEnvironment()).getContent();

        assertEquals(emptyList, result.getEnvironment().getProperty(SUBMODELS));
        assertEquals(emptyList, result.getEnvironment().getProperty(SHELLS));
        assertEquals(emptyList, result.getEnvironment().getProperty(CONCEPT_DESCRIPTIONS));
    }

    @Test
    void testOnlySubmodels() {
        onlySubmodelsDecider = true;

        var env = getEnvironment();

        var result = testSubject.executeSingle(new Service.Builder().withUrl(accessUrl).build(), env).getContent();

        assertEquals(env.getSubmodels().size(), getChildren(result.getEnvironment(), SUBMODELS).size());
        assertEquals(emptyList, result.getEnvironment().getProperty(SHELLS));
        assertEquals(emptyList, result.getEnvironment().getProperty(CONCEPT_DESCRIPTIONS));
    }

    @Test
    void testWholeEnvironmentIdEquality() {
        var env = getEnvironment();

        var result = testSubject.executeSingle(new Service.Builder().withUrl(accessUrl).build(), env);

        assertTrue(result.succeeded());

        var res = result.getContent().getEnvironment();

        assertEquals(env.getSubmodels().size(), getChildren(res, SUBMODELS).size());

        assertEquals(env.getAssetAdministrationShells().size(), getChildren(res, SHELLS).size());

        assertEquals(env.getConceptDescriptions().size(), getChildren(res, CONCEPT_DESCRIPTIONS).size());
    }

    @Test
    void testCorrectAccessUrlsHttpDataAddress() {
        var configController = new ConfigurationController(ConfigFactory.empty(), new ConsoleMonitor());
        configController.updateConfiguration("{\"edc.aas.useAasDataPlane\":\"false\"}");

        var env = getEnvironment();
        var result = testSubject.executeSingle(new Service.Builder().withUrl(accessUrl).build(), env).getContent();
        var shellDataAddress =
                (HttpDataAddress) getChildren(result.getEnvironment(), SHELLS).stream().map(Asset::getDataAddress).toList().get(0);
        var submodelDataAddress =
                (HttpDataAddress) getChildren(result.getEnvironment(), SUBMODELS).stream().map(Asset::getDataAddress).toList().get(0);
        var conceptDescriptionDataAddress =
                (HttpDataAddress) getChildren(result.getEnvironment(), CONCEPT_DESCRIPTIONS).stream().map(Asset::getDataAddress).toList().get(0);

        assertTrue(shellDataAddress.getBaseUrl().startsWith(accessUrl.toString()));
        assertEquals("%s/%s".formatted(SHELLS,
                        Base64.getEncoder().encodeToString(env.getAssetAdministrationShells().get(0).getId().getBytes())),
                shellDataAddress.getPath());
        assertTrue(submodelDataAddress.getBaseUrl().startsWith(accessUrl.toString()));
        assertEquals("%s/%s".formatted(SUBMODELS,
                        Base64.getEncoder().encodeToString(env.getSubmodels().get(0).getId().getBytes())),
                submodelDataAddress.getPath());
        assertTrue(conceptDescriptionDataAddress.getBaseUrl().startsWith(accessUrl.toString()));
        assertEquals("concept-descriptions/%s".formatted(Base64.getEncoder().encodeToString(env.getConceptDescriptions().get(0).getId().getBytes())), conceptDescriptionDataAddress.getPath());
    }

    @Test
    void testCorrectAccessUrlsAasDataAddress() {
        var configController = new ConfigurationController(ConfigFactory.empty(), new ConsoleMonitor());
        configController.updateConfiguration("{\"edc.aas.useAasDataPlane\":\"true\"}");

        var env = getEnvironment();
        var result = testSubject.executeSingle(new Service.Builder().withUrl(accessUrl).build(), env).getContent();
        var shellDataAddress =
                (AasDataAddress) getChildren(result.getEnvironment(), SHELLS).stream().map(Asset::getDataAddress).toList().get(0);
        var submodelDataAddress =
                (AasDataAddress) getChildren(result.getEnvironment(), SUBMODELS).stream().map(Asset::getDataAddress).toList().get(0);
        var conceptDescriptionDataAddress =
                (AasDataAddress) getChildren(result.getEnvironment(), CONCEPT_DESCRIPTIONS).stream().map(Asset::getDataAddress).toList().get(0);

        assertTrue(shellDataAddress.getBaseUrl().startsWith(accessUrl.toString()));
        assertEquals("%s/%s".formatted(SHELLS,
                        Base64.getEncoder().encodeToString(env.getAssetAdministrationShells().get(0).getId().getBytes())),
                shellDataAddress.getPath());
        assertTrue(submodelDataAddress.getBaseUrl().startsWith(accessUrl.toString()));
        assertEquals("%s/%s".formatted(SUBMODELS,
                        Base64.getEncoder().encodeToString(env.getSubmodels().get(0).getId().getBytes())),
                submodelDataAddress.getPath());
        assertTrue(conceptDescriptionDataAddress.getBaseUrl().startsWith(accessUrl.toString()));
        assertEquals("concept-descriptions/%s".formatted(Base64.getEncoder().encodeToString(env.getConceptDescriptions().get(0).getId().getBytes())), conceptDescriptionDataAddress.getPath());
    }

    @SuppressWarnings("unchecked")
    private List<Asset> getChildren(Asset parent, String name) {
        return (List<Asset>) parent.getProperty(name);
    }

}