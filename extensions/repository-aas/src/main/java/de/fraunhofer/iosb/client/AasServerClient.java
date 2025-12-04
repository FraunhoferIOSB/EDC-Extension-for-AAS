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
package de.fraunhofer.iosb.client;

import org.eclipse.digitaltwin.aas4j.v3.model.Reference;

import java.net.URI;
import java.util.Map;


/**
 * An AAS server client is used for communications with an AAS server. AAS servers can be AAS repositories, Submodel repositories, Concept-Description repositories, AAS
 * registries.
 */
public interface AasServerClient {

    /**
     * Returns whether the AAS server is reachable via its URL.
     *
     * @return Whether the AAS server is reachable.
     */
    boolean isAvailable();


    /**
     * Returns the URI used to reach the AAS server.
     *
     * @return The URI of the AAS server.
     */
    URI getUri();


    /**
     * Returns whether a given reference is eligible for registration. An element is eligible if all elements are to be registered or a selection of elements are to be registered
     * and this element is part of that selection.
     *
     * @param reference Reference of the element whose eligibility is to be checked
     * @return Whether the element behind the reference is eligible for registration
     */
    boolean eligibleForRegistration(Reference reference);


    /**
     * Returns whether the AAS server requires some kind of authentication when communicating via its URL.
     *
     * @return Whether the AAS server requires authentication.
     */
    default boolean requiresAuthentication() {
        return false;
    }


    /**
     * Returns potentially required additional headers for authenticating a request to the AAS server.
     *
     * @return Authentication headers.
     */
    default Map<String, String> getHeaders() {
        return Map.of();
    }
}
