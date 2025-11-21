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

import de.fraunhofer.iosb.app.stores.repository.AasServerStore;
import de.fraunhofer.iosb.app.testutils.MockServerTestExtension;
import de.fraunhofer.iosb.ilt.faaast.service.model.api.paging.Page;
import de.fraunhofer.iosb.ilt.faaast.service.model.api.paging.PagingMetadata;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Extension;
import org.eclipse.digitaltwin.aas4j.v3.model.Identifiable;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.defaults.storage.assetindex.InMemoryAssetIndex;
import org.eclipse.edc.connector.controlplane.defaults.storage.contractdefinition.InMemoryContractDefinitionStore;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public abstract class AbstractAasServerControllerIT<C extends AbstractAasServerController> extends MockServerTestExtension {
    protected AssetIndex assetIndex;
    protected ContractDefinitionStore contractDefinitionStore;
    protected AasServerStore repository;

    protected C testSubject;


    protected static void assertSelfDescription(Environment selfDescription) {
        assertNotNull(selfDescription);
        assertNotNull(selfDescription.getAssetAdministrationShells());
        assertNotNull(selfDescription.getSubmodels());
        assertNotNull(selfDescription.getConceptDescriptions());
    }


    @BeforeEach
    void setUp() {
        repository = new AasServerStore();
        var criterionRegistry = CriterionOperatorRegistryImpl.ofDefaults();
        assetIndex = new InMemoryAssetIndex(criterionRegistry);
        contractDefinitionStore = new InMemoryContractDefinitionStore(criterionRegistry);
        testSubject = getTestSubject();
    }


    protected abstract C getTestSubject();


    protected <T extends Identifiable> void assertIdentifiables(List<T> identifiablesShould, List<T> identifiablesIs) {
        assertEquals(identifiablesIs.size(), identifiablesShould.size());

        List<Extension> extensionsIs = new ArrayList<>();
        for (T aas: identifiablesIs) {
            // In this test, they don't have extensions beforehand.
            extensionsIs.add(aas.getExtensions().remove(0));
            assertTrue(identifiablesShould.contains(aas));
        }

        assertEquals(identifiablesShould.size(), extensionsIs.size());
        List<String> assetIds = extensionsIs.stream().map(Extension::getValue).toList();
        assetIds.forEach(this::assertStores);
    }


    protected void assertStores(String assetId) {
        var assets = assetIndex.queryAssets(QuerySpec.max()).toList();

        assets.stream().filter(a -> assetId.equals(a.getId())).findAny().orElseThrow();

        var contractDefinitions = contractDefinitionStore.findAll(QuerySpec.max()).toList();

        assertEquals(1, contractDefinitions.size());
        var contractDefinitionAssetIds = (List<?>) contractDefinitions.get(0).getAssetsSelector().get(0).getOperandRight();
        assertTrue(contractDefinitionAssetIds.contains(assetId));
    }


    protected Page<?> emptyPage() {
        return Page.builder()
                .result(List.of())
                .metadata(PagingMetadata.builder().build())
                .build();
    }


    protected <T> Page<T> asPage(List<T> ts) {
        return Page.<T> builder()
                .result(ts)
                .metadata(PagingMetadata.builder().build())
                .build();
    }

}
