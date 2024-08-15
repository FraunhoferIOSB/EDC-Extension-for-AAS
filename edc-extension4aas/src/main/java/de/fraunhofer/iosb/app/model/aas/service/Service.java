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
package de.fraunhofer.iosb.app.model.aas.service;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;

/**
 * An AAS service as seen in <a href="https://github.com/FraunhoferIOSB/FAAAST-Service">FA³ST Service</a>
 *
 * @param accessUrl   URL for accessing the service.
 * @param environment The AAS environment in asset form.
 */
public record Service(URL accessUrl, Asset environment) {

    /**
     * Only checks for accessUrl!
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Service service = (Service) o;
        try {
            return Objects.equals(accessUrl.toURI(), service.accessUrl.toURI());
        } catch (URISyntaxException e) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(accessUrl);
    }
}
