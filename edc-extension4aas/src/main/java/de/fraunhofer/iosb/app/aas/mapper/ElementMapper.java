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
package de.fraunhofer.iosb.app.aas.mapper;

import de.fraunhofer.iosb.aas.lib.model.AasProvider;
import de.fraunhofer.iosb.aas.lib.model.impl.Service;
import de.fraunhofer.iosb.aas.lib.spi.AasDataAddress;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.Objects;

/**
 * Contains base logic for mapping AAS elements to Assets
 */
public class ElementMapper {


    protected ElementMapper() {
    }

    protected @NotNull String generateId(DataAddress dataAddress) {
        if (dataAddress.getType().equals("AasData")) {
            var aasDataAddress = (AasDataAddress) dataAddress;
            return String.valueOf("%s:%s".formatted((aasDataAddress.getBaseUrl()), aasDataAddress.getPath()).hashCode());
        } else if (dataAddress.getType().equals("HttpData")) {
            var httpDataAddress = (HttpDataAddress) dataAddress;

            return String.valueOf("%s:%s".formatted(httpDataAddress.getBaseUrl(), httpDataAddress.getPath()).hashCode());
        } else {
            String idProperty = "id";
            if (dataAddress.hasProperty(idProperty)) {
                return Objects.requireNonNull(dataAddress.getStringProperty(idProperty));
            }
            throw new IllegalArgumentException(String.format("ID could not be inferred from DataAddress %s", dataAddress));
        }
    }

    protected DataAddress createDataAddress(AasProvider provider, Reference reference) {
        return AasDataAddress.Builder.newInstance()
                .aasProvider(provider)
                .referenceChain(reference)
                .build();
    }

    protected DataAddress createDataAddress(URL href) {
        return AasDataAddress.Builder.newInstance()
                .aasProvider(new Service.Builder().withUrl(href).build())
                .build();
    }
}
