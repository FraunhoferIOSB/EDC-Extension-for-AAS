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
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonSerializer;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static de.fraunhofer.iosb.aas.lib.model.impl.Service.CONCEPT_DESCRIPTIONS_PATH;
import static de.fraunhofer.iosb.aas.lib.model.impl.Service.SHELLS_PATH;
import static de.fraunhofer.iosb.aas.lib.model.impl.Service.SUBMODELS_PATH;
import static de.fraunhofer.iosb.aas.lib.type.AasConstants.AAS_V30_NAMESPACE;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;
import static org.eclipse.edc.dataaddress.httpdata.spi.HttpDataAddressSchema.BASE_URL;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Inspired by org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress
 * Enables more specific communication with AAS services
 */
@JsonTypeName
@JsonDeserialize(builder = DataAddress.Builder.class)
public class AasDataAddress extends DataAddress {

    public static final String AAS_DATA_TYPE = "AasData";
    public static final String PROXY_OPERATION = AAS_V30_NAMESPACE + "proxyOperation";
    public static final String PROXY_METHOD = AAS_V30_NAMESPACE + "proxyMethod";
    public static final String PROXY_PATH = AAS_V30_NAMESPACE + "proxyPath";
    public static final String PROXY_BODY = AAS_V30_NAMESPACE + "proxyBody";
    // See aas4j operation
    private static final String ADDITIONAL_HEADER = "aas:header:";
    private static final String METHOD = EDC_NAMESPACE + "method";
    private static final String REFERENCE_CHAIN = AAS_V30_NAMESPACE + "referenceChain";
    private static final String PATH = AAS_V30_NAMESPACE + "path";

    private static final JsonSerializer jsonSerializer = new JsonSerializer();
    private static final JsonDeserializer jsonDeserializer = new JsonDeserializer();

    private AasDataAddress() {
        super();
        this.setType(AAS_DATA_TYPE);
    }

    @JsonIgnore
    public String getBaseUrl() {
        return getStringProperty(BASE_URL);
    }

    @JsonIgnore
    public String getMethod() {
        return getStringProperty(METHOD, "GET");
    }

    @JsonIgnore
    public Map<String, String> getAdditionalHeaders() {
        return getProperties().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(ADDITIONAL_HEADER))
                .collect(toMap(entry -> entry.getKey().replace(ADDITIONAL_HEADER, ""), it -> (String) it.getValue()));
    }

    /**
     * If an explicit path is available, return this path. Else, return the
     * following:
     * <p>
     * build and returns the HTTP URL path required to access this AAS data at the
     * AAS service.
     * Example: ReferenceChain: [Submodel x, SubmodelElementCollection y,
     * SubmodelElement z]
     * --> path: submodels/base64(x)/submodel-elements/y.z
     *
     * @return Explicitly defined path or path correlating to reference chain stored
     *         in this DataAddress (no leading '/').
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
                case SUBMODEL_ELEMENT, SUBMODEL_ELEMENT_COLLECTION, SUBMODEL_ELEMENT_LIST, PROPERTY,
                     ANNOTATED_RELATIONSHIP_ELEMENT, RELATIONSHIP_ELEMENT, DATA_ELEMENT, MULTI_LANGUAGE_PROPERTY, RANGE, FILE, BLOB,
                     REFERENCE_ELEMENT, CAPABILITY, ENTITY, EVENT_ELEMENT, BASIC_EVENT_ELEMENT, OPERATION ->
                        new String[]{ urlBuilder.indexOf("/submodel-elements/") == -1 ? "/submodel-elements/".concat(value)
                                : ".".concat(value) };
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

    public Reference getReferenceChain() {
        Reference referenceChain = null;

        String referenceString = getStringProperty(REFERENCE_CHAIN);
        if (referenceString == null) {
            return new DefaultReference();
        }

        try {
            referenceChain = jsonDeserializer.read(getStringProperty(REFERENCE_CHAIN), Reference.class);
        } catch (DeserializationException e) {
            throw new EdcException(new IllegalStateException(("Faulty reference chain: %s").formatted(referenceChain)));
        }

        return referenceChain;
    }

    public HttpDataAddress asHttpDataAddress() {
        HttpDataAddress.Builder httpDataAddress = HttpDataAddress.Builder.newInstance();
        this.getAdditionalHeaders().forEach(httpDataAddress::addAdditionalHeader);

        return httpDataAddress
                .baseUrl(this.getBaseUrl())
                .method(this.getMethod())
                .path(this.getPath())
                .build();
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
            this.property(BASE_URL, provider.baseUrl().toString());
            provider.getHeaders().forEach((k, v) -> this.property(ADDITIONAL_HEADER + k, v));
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

        public Builder proxyOperation(String operation) {
            this.property(PROXY_OPERATION, operation);
            return this;
        }

        public Builder proxyBody(String proxyBody) {
            this.property(PROXY_BODY, proxyBody);
            return this;
        }

        public Builder proxyMethod(String proxyMethod) {
            this.property(PROXY_METHOD, proxyMethod);
            return this;
        }

        public Builder proxyPath(String proxyPath) {
            this.property(PROXY_PATH, proxyPath);
            return this;
        }

        public Builder referenceChain(Reference referenceChain) {
            try {
                this.property(REFERENCE_CHAIN, jsonSerializer.write(referenceChain));
            } catch (SerializationException e) {
                throw new EdcException(e);
            }
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
