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
import de.fraunhofer.iosb.aas.lib.spi.AasDataAddress;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import org.eclipse.digitaltwin.aas4j.v3.model.Identifiable;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.Referable;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAdministrativeInformation;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultKey;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Contains base logic for mapping AAS elements to Assets
 */
public class ElementMapper {

    private final Supplier<Boolean> useAasDataAddress = () -> Configuration.getInstance().isUseAasDataPlane();

    protected ElementMapper() {
    }

    protected <R extends Referable> Asset.Builder mapReferableToAssetBuilder(R referable) {
        return Asset.Builder.newInstance()
                .properties(Map.of(
                        "idShort", Optional.ofNullable(referable.getIdShort()).orElse(""),
                        "name", referable.getDisplayName(),
                        "description", referable.getDescription()));
    }

    protected <I extends Identifiable> Asset.Builder mapIdentifiableToAssetBuilder(I identifiable) {
        var admin = Optional.ofNullable(identifiable.getAdministration())
                .orElse(new DefaultAdministrativeInformation.Builder().build());
        var version = null != admin.getVersion() && null != admin.getRevision() ?
                String.valueOf(admin.getVersion()).concat(":").concat(String.valueOf(admin.getRevision())) : null;

        return mapReferableToAssetBuilder(identifiable)
                .version(version)
                .contentType("application/json")
                .properties(Map.of(
                        "id", identifiable.getId(),
                        "embeddedDataSpecifications", admin.getEmbeddedDataSpecifications()));
    }


    protected @NotNull String getId(DataAddress dataAddress) {
        if (dataAddress.getType().equals("AasData")) {
            var aasDataAddress = (AasDataAddress) dataAddress;
            return String.valueOf("%s:%s".formatted((aasDataAddress.getAccessUrl().getContent().toString()),
                    aasDataAddress.getPath()).hashCode());
        } else if (dataAddress.getType().equals("HttpData")) {
            var httpDataAddress = (HttpDataAddress) dataAddress;

            return String.valueOf("%s:%s".formatted(httpDataAddress.getBaseUrl(),
                    httpDataAddress.getPath()).hashCode());
        } else {
            String idProperty = "id";
            if (dataAddress.hasProperty(idProperty)) {
                return Objects.requireNonNull(dataAddress.getStringProperty(idProperty));
            }
            throw new IllegalArgumentException(String.format("ID could not be inferred from DataAddress %s", dataAddress));
        }
    }

    protected DataAddress createDataAddress(AasProvider provider, Reference reference) {
        var aasDataAddress = AasDataAddress.Builder.newInstance()
                .aasProvider(provider)
                .referenceChain(reference)
                .build();
        if (this.useAasDataAddress.get()) {
            return aasDataAddress;
        } else {
            var httpDataAddress = HttpDataAddress.Builder.newInstance();
            provider.getHeaders().forEach(httpDataAddress::addAdditionalHeader);
            return httpDataAddress
                    .baseUrl(aasDataAddress.getAccessUrl().getContent().toString())
                    .method(aasDataAddress.getMethod())
                    .path(aasDataAddress.getPath())
                    .build();
        }

    }

    protected Reference createReference(KeyTypes type, String value) {
        return new DefaultReference.Builder()
                .keys(new DefaultKey.Builder().type(type).value(value).build())
                .build();
    }

    protected Reference createReference(String value, Reference parent) {
        return new DefaultReference.Builder()
                .keys(new ArrayList<>(parent.getKeys()))
                .keys(new DefaultKey.Builder().type(KeyTypes.SUBMODEL_ELEMENT).value(value).build())
                .build();
    }
}
