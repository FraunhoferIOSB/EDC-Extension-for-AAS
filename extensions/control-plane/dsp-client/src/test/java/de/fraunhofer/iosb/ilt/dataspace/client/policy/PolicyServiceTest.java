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
package de.fraunhofer.iosb.ilt.dataspace.client.policy;

import de.fraunhofer.iosb.ilt.dataspace.client.config.CatalogConfiguration;
import de.fraunhofer.iosb.ilt.dataspace.client.store.CounterPartyAddressStore;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class PolicyServiceTest {

    private static final String PARTICIPANT_CONTEXT_ID = "participant-context-id";
    private static final String COUNTER_PARTY_ID = "counter-party-id";
    private static final String COUNTER_PARTY_URI = "https://provider.example.com/dsp";
    private static final String ASSET_ID = "asset-id";
    private static final String OFFER_ID = "offer-id";
    private static final byte[] CATALOG_JSON_BYTES = "{\"id\": \"catalog\"}".getBytes();

    private CatalogService catalogService;
    private ParticipantContextStore participantContextStore;
    private TypeTransformerRegistry transformer;
    private CatalogConfiguration configuration;
    private AcceptedPolicyStore acceptedPolicyStore;
    private JsonLd jsonLd;
    private CounterPartyAddressStore counterPartyAddressStore;
    private PolicyService testSubject;
    private Policy policy;


    @BeforeEach
    void setUp() {
        catalogService = mock(CatalogService.class);
        participantContextStore = mock(ParticipantContextStore.class);
        transformer = mock(TypeTransformerRegistry.class);
        configuration = mock(CatalogConfiguration.class);
        acceptedPolicyStore = mock(AcceptedPolicyStore.class);
        jsonLd = mock(JsonLd.class);
        counterPartyAddressStore = mock(CounterPartyAddressStore.class);
        policy = mock(Policy.class);
        testSubject = new PolicyService(catalogService, participantContextStore, transformer, configuration, acceptedPolicyStore, jsonLd, counterPartyAddressStore);

        var participantContext = mock(ParticipantContext.class);
        var catalog = mock(Catalog.class);
        var dataset = mock(Dataset.class);
        var expandedCatalog = mock(JsonObject.class);

        when(participantContextStore.findById(any())).thenReturn(StoreResult.success(participantContext));
        when(catalogService.requestCatalog(any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(StatusResult.success(CATALOG_JSON_BYTES)));
        when(jsonLd.expand(any(JsonObject.class))).thenReturn(Result.success(expandedCatalog));
        when(transformer.transform(any(JsonObject.class), eq(Catalog.class))).thenReturn(Result.success(catalog));
        when(catalog.getDatasets()).thenReturn(List.of(dataset));
        when(dataset.getOffers()).thenReturn(Map.of(OFFER_ID, policy));
        when(policy.withTarget(ASSET_ID)).thenReturn(policy);
        when(configuration.getCatalogTimeout()).thenReturn(5);
    }


    @Test
    void getDatasetReturnsAcceptableOfferTest() {
        when(configuration.acceptAllPolicies()).thenReturn(true);

        ServiceResult<ContractOffer> result = testSubject.getDataset(PARTICIPANT_CONTEXT_ID, COUNTER_PARTY_ID, COUNTER_PARTY_URI, ASSET_ID);

        assertTrue(result.succeeded());
        assertEquals(OFFER_ID, result.getContent().getId());
        assertEquals(ASSET_ID, result.getContent().getAssetId());
        assertEquals(policy, result.getContent().getPolicy());
        verify(catalogService).requestCatalog(any(), eq(COUNTER_PARTY_ID), eq(COUNTER_PARTY_URI), anyString(), any());
    }


    @Test
    void getDatasetFailsWhenNoPolicyIsAcceptableTest() {
        when(configuration.acceptAllPolicies()).thenReturn(false);
        when(acceptedPolicyStore.getPolicies()).thenReturn(List.of());

        ServiceResult<ContractOffer> result = testSubject.getDataset(PARTICIPANT_CONTEXT_ID, COUNTER_PARTY_ID, COUNTER_PARTY_URI, ASSET_ID);

        assertTrue(result.failed());
        assertTrue(result.getFailureDetail().contains("Could not find acceptable policyDefinition"));
    }
}
