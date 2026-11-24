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
package de.fraunhofer.iosb.ilt.dataspace.client.testutils;

import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.response.StatusResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Stateful in-process fake for {@link CatalogService}. Serves the bytes arranged by the test ({@link #respondWith}
 * defaults to the {@code catalog.json} fixture) or a failure result ({@link #failWith}), records the parameters of the
 * last catalog request and how many catalog requests were made. All fakes in this package follow the plan's
 * stateful-fake convention instead of Mockito mocks so the production async flow can be driven and inspected.
 */
public class FakeCatalogService implements CatalogService {

    private final AtomicReference<byte[]> catalogBytes = new AtomicReference<>(CatalogFixtures.catalogBytes());
    private final AtomicReference<StatusResult<byte[]>> failureResponse = new AtomicReference<>();
    private final AtomicReference<CapturedCatalogRequest> capturedRequest = new AtomicReference<>();
    private final AtomicInteger requestCount = new AtomicInteger();

    /**
     * Sets the catalog JSON-LD bytes returned by the next catalog request and clears a possibly arranged failure.
     */
    public void respondWith(byte[] bytes) {
        catalogBytes.set(bytes);
        failureResponse.set(null);
    }


    /**
     * Arranges a failing catalog response, as if the catalog could not be retrieved from the counter-party.
     */
    public void failWith(StatusResult<byte[]> failure) {
        failureResponse.set(failure);
    }


    /**
     * Restores the default fixture-based response and clears all recorded state.
     */
    public void reset() {
        respondWith(CatalogFixtures.catalogBytes());
        capturedRequest.set(null);
        requestCount.set(0);
    }


    public CapturedCatalogRequest capturedRequest() {
        return capturedRequest.get();
    }


    public int requestCount() {
        return requestCount.get();
    }


    @Override
    public CompletableFuture<StatusResult<byte[]>> requestCatalog(ParticipantContext participantContext, String counterPartyId, String counterPartyAddress, String protocol,
                                                                  QuerySpec querySpec, String... additionalScopes) {
        requestCount.incrementAndGet();
        capturedRequest.set(new CapturedCatalogRequest(counterPartyId, counterPartyAddress, protocol, querySpec));
        var failure = failureResponse.get();
        var response = failure != null ? failure : StatusResult.success(catalogBytes.get());
        return CompletableFuture.completedFuture(response);
    }


    @Override
    public CompletableFuture<StatusResult<byte[]>> requestDataset(ParticipantContext participantContext, String id, String counterPartyId, String counterPartyAddress,
                                                                  String protocol) {
        throw new UnsupportedOperationException("requestDataset is not part of the dsp-client negotiate flow");
    }

    /**
     * The parameters of the last catalog request issued by the production {@code PolicyService}.
     */
    public record CapturedCatalogRequest(String counterPartyId, String counterPartyAddress, String protocol, QuerySpec querySpec) {}
}
