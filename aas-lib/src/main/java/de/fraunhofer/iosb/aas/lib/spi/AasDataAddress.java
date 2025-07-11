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
package de.fraunhofer.iosb.aas.lib.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import de.fraunhofer.iosb.aas.lib.model.AasProvider;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static de.fraunhofer.iosb.aas.lib.model.impl.Service.CONCEPT_DESCRIPTIONS_PATH;
import static de.fraunhofer.iosb.aas.lib.model.impl.Service.SHELLS_PATH;
import static de.fraunhofer.iosb.aas.lib.model.impl.Service.SUBMODELS_PATH;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;


/**
 * Inspired by  org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress
 * Enables more specific communication with AAS services
 */
@JsonTypeName
@JsonDeserialize(builder = DataAddress.Builder.class)
public class AasDataAddress extends DataAddress {

    public static final String AAS_DATA_TYPE = "AasData";

    // See aas4j operation
    public static final String OPERATION = "operation";
    private static final String ADDITIONAL_HEADER = "header:";
    private static final String METHOD = "method";
    private static final String PROVIDER = "AAS-Provider";
    private static final String REFERENCE_CHAIN = "referenceChain";
    private static final String PATH = "PATH";

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
    public Result<URL> getAccessUrl() {
        if (hasProvider()) {
            return Result.success(getProvider().getAccessUrl());
        }
        return Result.failure("No URL available for this AasDataAddress");
    }

    private boolean hasProvider() {
        return getProperties().get(PROVIDER) != null;
    }

    private AasProvider getProvider() {
        Object provider = super.getProperties().get(PROVIDER);
        if (provider instanceof AasProvider) {
            return (AasProvider) provider;
        }
        throw new EdcException(new IllegalStateException("Provider not set correctly: %s".formatted(provider)));
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
                .collect(toMap(headerName -> headerName.getKey().replace(ADDITIONAL_HEADER, ""), headerValue -> (String) headerValue.getValue())));
        return headers;
    }

    /**
     * If an explicit path is available, return this path. Else, return the following:
     * <p>
     * build and returns the HTTP URL path required to access this AAS data at the AAS service.
     * Example: ReferenceChain: [Submodel x, SubmodelElementCollection y, SubmodelElement z]
     * --> path: submodels/base64(x)/submodel-elements/y.z
     *
     * @return Explicitly defined path or path correlating to reference chain stored in this DataAddress (no leading '/').
     */
    public String getPath() {
        return getStringProperty(PATH, referenceChainAsPath());
    }

    private String referenceChainAsPath() {
        StringBuilder urlBuilder = new StringBuilder();

        for (var key : getReferenceChain().getKeys()) {
            var value = key.getValue();
            String[] toAppend = switch (key.getType()) {
                case ASSET_ADMINISTRATION_SHELL -> new String[]{ SHELLS_PATH, b64(value) };
                case SUBMODEL -> new String[]{ SUBMODELS_PATH, b64(value) };
                case CONCEPT_DESCRIPTION -> new String[]{ CONCEPT_DESCRIPTIONS_PATH, b64(value) };
                case SUBMODEL_ELEMENT, SUBMODEL_ELEMENT_COLLECTION, SUBMODEL_ELEMENT_LIST ->
                        new String[]{ urlBuilder.indexOf("/submodel-elements/") == -1 ?
                                "/submodel-elements/".concat(value) : ".".concat(value) };
                default -> throw new EdcException(new IllegalStateException("Element type not recognized: %s".formatted(key)));
            };

            urlBuilder.append(String.join("/", toAppend));
        }

        return urlBuilder.toString();
    }

    /* Return base64 encoded String version of input */
    private String b64(String toBeEncoded) {
        Objects.requireNonNull(toBeEncoded, "toBeEncoded must not be null");
        return Base64.getEncoder().encodeToString(toBeEncoded.getBytes());
    }

    private Reference getReferenceChain() {
        var referenceChain = properties.get(REFERENCE_CHAIN);

        if (referenceChain == null) {
            return new DefaultReference();
        }

        if (referenceChain instanceof Reference reference && reference.getKeys() != null) {
            return reference;
        }

        throw new EdcException(new IllegalStateException(("Faulty reference chain: %s").formatted(referenceChain)));
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

        public Builder path(String path) {
            this.property(PATH, path);
            return this;
        }

        public Builder method(String method) {
            this.property(METHOD, method);
            return this;
        }

        /*
            Why not use Operation.class or InputVariable.class/InOutputVariable.class?
            - Values of any type other than String get removed when sending the DA from
              consumer to provider (during "compaction" phase when serializing the DA)
         */
        public Builder operation(String operation) {
            this.property(OPERATION, operation);
            return this;
        }

        public Builder referenceChain(Reference referenceChain) {
            this.property(REFERENCE_CHAIN, referenceChain);
            return this;
        }

        public Builder copyFrom(DataAddress other) {
            Optional.ofNullable(other).map(DataAddress::getProperties).orElse(emptyMap()).forEach(this::property);
            return this;
        }

        @Override
        public AasDataAddress build() {
            this.type(AAS_DATA_TYPE);
            return address;
        }
    }

}
