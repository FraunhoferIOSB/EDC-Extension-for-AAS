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
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultKey;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Supplier;

import static de.fraunhofer.iosb.aas.lib.type.AasConstants.AAS_V30_NAMESPACE;

/**
 * Contains base logic for mapping AAS elements to Assets
 */
public class ElementMapper {


    private final Supplier<Boolean> useAasDataAddress = () -> Configuration.getInstance().isUseAasDataPlane();

    protected ElementMapper() {
    }

    protected <R extends Referable> Asset.Builder mapReferableToAssetBuilder(R referable) {
        var referableNamespace = AAS_V30_NAMESPACE.concat("Referable/");
        var assetBuilder = Asset.Builder.newInstance();

        if (referable.getIdShort() != null && !referable.getIdShort().isEmpty()) {
            assetBuilder.property(referableNamespace.concat("idShort"), referable.getIdShort());
        }

        if (referable.getDisplayName() != null && !referable.getDisplayName().isEmpty()) {
            assetBuilder.property(referableNamespace.concat("displayName"), referable.getDisplayName());
        }

        if (referable.getDescription() != null && !referable.getDescription().isEmpty()) {
            assetBuilder.property(referableNamespace.concat("description"), referable.getDescription());
        }

        return assetBuilder;
    }

    protected <I extends Identifiable> Asset.Builder mapIdentifiableToAssetBuilder(I identifiable) {
        var identifiableNamespace = AAS_V30_NAMESPACE.concat("Identifiable/");

        var admin = identifiable.getAdministration();

        var assetBuilder = mapReferableToAssetBuilder(identifiable)
                .contentType("application/json")
                .property(identifiableNamespace.concat("id"),
                        identifiable.getId());

        if (admin == null) {
            return assetBuilder;
        }

        if (admin.getEmbeddedDataSpecifications() != null && !admin.getEmbeddedDataSpecifications().isEmpty()) {
            assetBuilder.property(AAS_V30_NAMESPACE + "HasDataSpecification/" + "embeddedDataSpecifications", admin.getEmbeddedDataSpecifications());
        }

        if (admin.getVersion() != null) {
            assetBuilder.property(AAS_V30_NAMESPACE + "AdministrativeInformation/" + "version", admin.getVersion());
        }

        if (admin.getRevision() != null) {
            assetBuilder.property(AAS_V30_NAMESPACE + "AdministrativeInformation/" + "revision", admin.getRevision());
        }

        return assetBuilder;
    }


    protected @NotNull String getId(DataAddress dataAddress) {
        if (dataAddress.getType().equals("AasData")) {
            var aasDataAddress = (AasDataAddress) dataAddress;
            return String.valueOf("%s:%s".formatted((aasDataAddress.getAccessUrl().getContent().toString()), aasDataAddress.getPath()).hashCode());
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
