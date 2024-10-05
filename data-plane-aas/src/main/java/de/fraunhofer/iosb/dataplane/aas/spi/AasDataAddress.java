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
package de.fraunhofer.iosb.dataplane.aas.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import de.fraunhofer.iosb.model.aas.AasProvider;
import de.fraunhofer.iosb.util.Encoder;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.fraunhofer.iosb.dataplane.aas.pipeline.AasDataSourceFactory.AAS_DATA_TYPE;
import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;


/**
 * Inspired by  org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress
 * Enables more specific communication with AAS services
 */
@JsonTypeName
@JsonDeserialize(builder = DataAddress.Builder.class)
public class AasDataAddress extends DataAddress {

    public static final String BASE_URL = "https://w3id.org/edc/v0.0.1/ns/baseUrl";

    private static final String ADDITIONAL_HEADER = "header:";
    private static final String METHOD = "method";
    private static final String PROVIDER = "AAS-Provider";
    private static final String REFERENCE_CHAIN = "referenceChain";
    private static final String PATH = "PATH";

    // See aas4j operation
    public static final String OPERATION = "operation";

    private AasDataAddress() {
        super();
        this.setType(AAS_DATA_TYPE);
    }

    public boolean isOperation() {
        return this.hasProperty(OPERATION);
    }

    public @Nullable String getOperation() {
        return isOperation() ? this.getStringProperty(OPERATION) : null;
    }

    @JsonIgnore
    public String getBaseUrl() {
        return hasProvider() ? getProvider().getAccessUrl().toString() : getStringProperty(BASE_URL);
    }

    private AasProvider getProvider() {
        Object provider = super.getProperties().get(PROVIDER);
        if (provider instanceof AasProvider) {
            return (AasProvider) provider;
        }
        throw new EdcException(new IllegalStateException("Provider not set correctly: %s".formatted(provider)));
    }

    private boolean hasProvider() {
        return getProperties().get(PROVIDER) != null;
    }

    @JsonIgnore
    public String getMethod() {
        return getStringProperty(METHOD);
    }

    @JsonIgnore
    public Map<String, String> getAdditionalHeaders() {
        // First get authentication headers from aas provider, then additional ones
        Map<String, String> headers = hasProvider() ? getProvider().getHeaders() : new HashMap<>();
        headers.putAll(getProperties().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(ADDITIONAL_HEADER))
                .collect(toMap(headerName -> headerName.getKey().replace(ADDITIONAL_HEADER, ""),
                        headerValue -> (String) headerValue.getValue())));
        return headers;
    }

    /**
     * If an explicit path is available, return this path. Else, return the following:
     * <p>
     * build and returns the HTTP URL path required to access this AAS data at the AAS service.
     * Example: ReferenceChain: [Submodel x, SubmodelElementCollection y, SubmodelElement z]
     * --> path: submodels/base64(x)/submodel-elements/y.z
     *
     * @return Explicitly defined path or path correlating to reference chain stored in this DataAddress (no leading
     * '/').
     */
    public String getPath() {
        return getStringProperty(PATH, referenceChainAsPath());
    }

    private String referenceChainAsPath() {
        StringBuilder urlBuilder = new StringBuilder();

        for (var key : getReferenceChain().getKeys()) {

            switch (key.getType()) {
                case ASSET_ADMINISTRATION_SHELL ->
                        urlBuilder.append("shells/").append(Encoder.encodeBase64(key.getValue()));
                case SUBMODEL -> urlBuilder.append("submodels/").append(Encoder.encodeBase64(key.getValue()));
                case CONCEPT_DESCRIPTION ->
                        urlBuilder.append("concept-descriptions/").append(Encoder.encodeBase64(key.getValue()));
                case SUBMODEL_ELEMENT, SUBMODEL_ELEMENT_COLLECTION, SUBMODEL_ELEMENT_LIST -> {
                    if (urlBuilder.indexOf("/submodel-elements/") == -1) {
                        urlBuilder.append("/submodel-elements/");
                    } else {
                        urlBuilder.append(".");
                    }
                    urlBuilder.append(key.getValue());
                }
                default -> throw new EdcException(new IllegalStateException(format("Element type not recognized in " +
                        "AasDataAddress: %s", key.getType())));
            }
        }

        return urlBuilder.toString();
    }

    private Reference getReferenceChain() {
        var referenceChain = properties.get(REFERENCE_CHAIN);

        if (Objects.isNull(referenceChain)) {
            return new DefaultReference();
        }

        if (referenceChain instanceof Reference) {
            return (Reference) referenceChain;
        }

        throw new EdcException(new IllegalStateException("Something not of type Reference was stored in the property " +
                "of an AasDataAddress!"));
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder extends DataAddress.Builder<AasDataAddress, Builder> {

        private Builder() {
            super(new AasDataAddress());
            this.property(METHOD, "GET");
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder aasProvider(AasProvider provider) {
            this.property(PROVIDER, provider);
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.property(BASE_URL, baseUrl);
            return this;
        }

        public Builder path(String path) {
            this.property(PATH, path);
            return this;
        }

        public Builder method(String method) {
            this.property(METHOD, method);
            return this;
        }

        public Builder operation(String operation) {
            // Why not input and inouputvariables? Because of the serialization of DataAddresses,
            // a direct list as property is not possible.
            // Why not Operation as type? Operation contains "raw lists"...
            // To send a DataAddress (serialization+compaction -> deserialization+expansion),
            // this value would get removed if it were of any type other than String
            this.property(OPERATION, operation);
            return this;
        }

        public Builder referenceChain(Reference referenceChain) {
            Objects.requireNonNull(referenceChain.getKeys());

            this.property(REFERENCE_CHAIN, referenceChain);
            return this;
        }

        public Builder copyFrom(DataAddress other) {
            Optional.ofNullable(other)
                    .map(DataAddress::getProperties)
                    .orElse(Collections.emptyMap())
                    .entrySet().stream().filter(entry -> entry.getKey().equals(OPERATION))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                    .forEach(this::property);
            return this;
        }

        @Override
        public AasDataAddress build() {
            this.type(AAS_DATA_TYPE);
            return address;
        }
    }

}
