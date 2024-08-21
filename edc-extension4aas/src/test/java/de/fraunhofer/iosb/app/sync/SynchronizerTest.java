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
package de.fraunhofer.iosb.app.sync;

import de.fraunhofer.iosb.app.aas.EnvironmentToAssetMapper;
import de.fraunhofer.iosb.app.model.ChangeSet;
import de.fraunhofer.iosb.app.util.Pair;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static de.fraunhofer.iosb.app.testutils.AasCreator.getEmptyEnvironment;
import static de.fraunhofer.iosb.app.testutils.AasCreator.getEnvironment;
import static de.fraunhofer.iosb.app.util.AssetUtil.flatMapAssets;
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

        var oldEnvironmentAsset = new EnvironmentToAssetMapper(() -> false).executeSingle(accessUrl, oldEnvironment);
        var newEnvironmentAsset = new EnvironmentToAssetMapper(() -> false).executeSingle(accessUrl, newEnvironment);

        var pair = new Pair<>(oldEnvironmentAsset.getContent().environment(), newEnvironmentAsset.getContent().environment());

        var result = testSubject.apply(List.of(pair));

        assertTrue(result.succeeded());
        assertNotNull(result.getContent());

        var shouldBe = new ChangeSet.Builder<Asset, String>()
                .add(flatMapAssets(newEnvironmentAsset.getContent().environment())).build();

        assertEquals(shouldBe, result.getContent());
    }


    @Test
    void testApplyRemoveSubmodel() {
        var oldEnvironment = getEnvironment();
        var newEnvironment = getEnvironment();

        var oldEnvironmentAsset = new EnvironmentToAssetMapper(() -> false).executeSingle(accessUrl, oldEnvironment);
        var newEnvironmentAsset = new EnvironmentToAssetMapper(() -> false).executeSingle(accessUrl, newEnvironment);

        var pair = new Pair<>(oldEnvironmentAsset.getContent().environment(), newEnvironmentAsset.getContent().environment());

        var result = testSubject.apply(List.of(pair));

        assertTrue(result.succeeded());
        assertNotNull(result.getContent());

        var shouldBe = new ChangeSet.Builder<Asset, String>()
                .add(flatMapAssets(newEnvironmentAsset.getContent().environment())).build();

        assertEquals(shouldBe, result.getContent());
    }
}
