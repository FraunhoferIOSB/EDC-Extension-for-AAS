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
package de.fraunhofer.iosb.client.authentication;

import de.fraunhofer.iosb.api.PublicApiManagementService;
import de.fraunhofer.iosb.api.model.Endpoint;
import de.fraunhofer.iosb.api.model.HttpMethod;
import de.fraunhofer.iosb.client.ClientEndpoint;

import java.util.List;
import java.util.Map;

/**
 * Custom AuthenticationRequestFilter filtering requests that go directly to an
 * AAS service (managed by this extension) or the extension's configuration.
 */
public class DataTransferEndpointManager {

    private final PublicApiManagementService publicApiManagementService;

    public DataTransferEndpointManager(PublicApiManagementService publicApiManagementService) {
        this.publicApiManagementService = publicApiManagementService;
    }

    /**
     * Add key,value pair for a request. This key will only be available for one
     * request.

     * @param agreementId Agreement to build the endpoint path suffix
     * @param key   The key name
     * @param value The value
     */
    public void addTemporaryEndpoint(String agreementId, String key, String value) {
        var endpointSuffix = ClientEndpoint.AUTOMATED_PATH + "/receiveData/" + agreementId;
        publicApiManagementService.addTemporaryEndpoint(new Endpoint(endpointSuffix, HttpMethod.POST, Map.of(key, List.of(value))));
    }
}
