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
package de.fraunhofer.iosb.ilt.app.sync;

import de.fraunhofer.iosb.ilt.aas.lib.model.impl.Service;
import de.fraunhofer.iosb.ilt.app.aas.EnvironmentToAssetMapper;
import de.fraunhofer.iosb.ilt.app.model.ChangeSet;
import de.fraunhofer.iosb.ilt.app.util.Pair;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEnvironment;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static de.fraunhofer.iosb.ilt.app.testutils.AasCreator.getEmptyEnvironment;
import static de.fraunhofer.iosb.ilt.app.testutils.AasCreator.getEmptySubmodel;
import static de.fraunhofer.iosb.ilt.app.testutils.AasCreator.getEnvironment;
import static de.fraunhofer.iosb.ilt.app.util.AssetUtil.flatMapAssets;
import static de.fraunhofer.iosb.ilt.app.util.AssetUtil.getChildren;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SynchronizerTest {

    private final URL accessUrl = new URL("https://localhost:%s".formatted(getFreePort()));

    private Synchronizer testSubject;

    public SynchronizerTest() throws MalformedURLException {
    }

    @BeforeEach
    public void setUp() {
        testSubject = new Synchronizer();
    }

    @Test
    void testApplyNull() {
        assertTrue(testSubject.apply(null).failed());
    }

    @Test
    void testApplyEmpty() {
        var result = testSubject.apply(List.of());
        assertTrue(result.succeeded());

        assertEquals(new ChangeSet.Builder<>().build(), result.getContent());
    }

    @Test
    void testApplyNewService() {
        var oldEnvironment = getEmptyEnvironment();
        var newEnvironment = getEnvironment();

        var oldEnvironmentAsset = new EnvironmentToAssetMapper(() -> false).executeSingle(new Service(accessUrl),
                oldEnvironment);
        var newEnvironmentAsset = new EnvironmentToAssetMapper(() -> false).executeSingle(new Service(accessUrl),
                newEnvironment);

        var pair = new Pair<>(oldEnvironmentAsset.getContent().environment(),
                newEnvironmentAsset.getContent().environment());

        var result = testSubject.apply(List.of(pair));

        assertTrue(result.succeeded());
        assertNotNull(result.getContent());

        var shouldBe =
                new ChangeSet.Builder<Asset, String>().add(flatMapAssets(newEnvironmentAsset.getContent().environment())).build();

        assertEquals(shouldBe, result.getContent());
    }

    @Test
    void testApplyRemoveSubmodel() {
        var oldEnvironment = getEnvironment();
        var oldSubmodels = new ArrayList<>(oldEnvironment.getSubmodels());
        var emptySubmodel = getEmptySubmodel();
        oldSubmodels.add(emptySubmodel);
        oldEnvironment.setSubmodels(oldSubmodels);

        var updatedSubmodels = new ArrayList<>(oldEnvironment.getSubmodels());
        updatedSubmodels.remove(emptySubmodel);

        var newEnvironment = new DefaultEnvironment.Builder()
                .assetAdministrationShells(oldEnvironment.getAssetAdministrationShells())
                .submodels(updatedSubmodels)
                .conceptDescriptions(oldEnvironment.getConceptDescriptions())
                .build();

        var oldEnvironmentAsset = new EnvironmentToAssetMapper(() -> false).executeSingle(new Service(accessUrl),
                oldEnvironment);
        var newEnvironmentAsset = new EnvironmentToAssetMapper(() -> false).executeSingle(new Service(accessUrl),
                newEnvironment);

        var pair = new Pair<>(oldEnvironmentAsset.getContent().environment(),
                newEnvironmentAsset.getContent().environment());

        var result = testSubject.apply(List.of(pair));

        assertTrue(result.succeeded());
        assertNotNull(result.getContent());

        var emptySubmodelId = getChildren(oldEnvironmentAsset.getContent().environment(), "submodels").stream()
                .filter(asset -> asset.getProperty("idShort").equals(emptySubmodel.getIdShort()))
                .findFirst().orElseThrow()
                .getId();

        assertTrue(result.getContent().toRemove().stream().anyMatch(emptySubmodelId::equals));
    }
}
