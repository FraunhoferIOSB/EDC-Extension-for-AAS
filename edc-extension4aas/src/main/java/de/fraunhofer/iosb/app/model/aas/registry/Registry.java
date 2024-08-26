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
package de.fraunhofer.iosb.app.model.aas.registry;

import de.fraunhofer.iosb.app.model.aas.AasAccessUrl;
import de.fraunhofer.iosb.app.model.aas.service.Service;

import java.util.Collection;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An AAS registry as seen in <a href="https://github.com/fraunhoferIOSB/FAAAST-registry">FA³ST Registry</a>
 *
 * @param accessUrl URL for accessing the registry.
 * @param services  The AAS services offered by this registry.
 */
public record Registry(@Nonnull AasAccessUrl accessUrl, @Nullable Collection<Service> services) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Registry registry = (Registry) o;

        return Objects.equals(accessUrl, registry.accessUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(accessUrl);
    }
}
