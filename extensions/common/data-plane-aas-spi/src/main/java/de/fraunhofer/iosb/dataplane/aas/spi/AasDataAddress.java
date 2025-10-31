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
import de.fraunhofer.iosb.ilt.faaast.service.util.ReferenceHelper;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonSerializer;
import org.eclipse.digitaltwin.aas4j.v3.model.Key;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.ReferenceTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static de.fraunhofer.iosb.constants.AasConstants.AAS_V30_NAMESPACE;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;
import static org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes.ASSET_ADMINISTRATION_SHELL;
import static org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes.CONCEPT_DESCRIPTION;
import static org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes.SUBMODEL;
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
    private static final String ADDITIONAL_HEADER = "aas:header:";
    private static final String METHOD = EDC_NAMESPACE + "method";
    private static final String REFERENCE_CHAIN = AAS_V30_NAMESPACE + "referenceChain";
    private static final String PATH = AAS_V30_NAMESPACE + "path";

    private static final JsonSerializer JSON_SERIALIZER = new JsonSerializer();
    private static final JsonDeserializer JSON_DESERIALIZER = new JsonDeserializer();
    private static final List<KeyTypes> IDENTIFIABLE_KEY_TYPES = List.of(ASSET_ADMINISTRATION_SHELL, SUBMODEL, CONCEPT_DESCRIPTION);

    private AasDataAddress() {
        super();
        this.setType(AAS_DATA_TYPE);
    }

    private static boolean validate(Reference reference) {
        if (reference == null || reference.getKeys() == null ||
                ReferenceTypes.EXTERNAL_REFERENCE == reference.getType() || reference.getKeys().isEmpty()) {
            return false;
        }

        Key root = ReferenceHelper.getRoot(reference);
        if (!IDENTIFIABLE_KEY_TYPES.contains(root.getType())) {
            return false;
        }


        if (reference.getKeys().size() == 1) {
            return reference.getKeys().get(0).getValue() != null;
        }

        for (Key key : reference.getKeys().subList(1, reference.getKeys().size())) {
            if (IDENTIFIABLE_KEY_TYPES.contains(key.getType())) {
                return false;
            }

            if (key.getType() == null || key.getValue() == null) {
                return false;
            }
        }
        return true;

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
                .collect(toMap(entry -> entry.getKey().replace(ADDITIONAL_HEADER, ""), it -> String.valueOf(it.getValue())));
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
        // Explicitly stored path takes precedence
        String explicitlyStoredPath = getStringProperty(PATH);
        if (explicitlyStoredPath != null) {
            return explicitlyStoredPath;
        }


        Reference reference = this.getReferenceChain();

        if (!validate(reference)) {
            throw new IllegalStateException(String.format("Malformed reference in AasDataAddress: %s", reference));
        }

        Key root = ReferenceHelper.getRoot(reference);

        String path = switch (root.getType()) {
            case SUBMODEL -> "submodels/";
            case ASSET_ADMINISTRATION_SHELL -> "shells/";
            case CONCEPT_DESCRIPTION -> "concept-descriptions/";
            default -> throw new IllegalStateException(String.format("Malformed reference in AasDataAddress: %s", reference));
        };

        path = path.concat(Base64.getEncoder().encodeToString(root.getValue().getBytes(StandardCharsets.UTF_8)));

        if (reference.getKeys().size() > 1) {
            path = path.concat("/submodel-elements/")
                    .concat(ReferenceHelper.toPath(reference));
        }

        return path;
    }

    public Reference getReferenceChain() {
        Reference referenceChain = null;

        String referenceString = getStringProperty(REFERENCE_CHAIN);
        if (referenceString == null) {
            return new DefaultReference();
        }

        try {
            referenceChain = JSON_DESERIALIZER.read(getStringProperty(REFERENCE_CHAIN), Reference.class);
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
        @SuppressWarnings("unchecked")
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder baseUrl(String baseUrl) {
            this.property(BASE_URL, baseUrl);
            return this;
        }

        public Builder additionalHeaders(Map<String, String> headers) {
            headers.forEach((k, v) -> this.property(ADDITIONAL_HEADER + k, v));
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

        /**
         * As we only store the reference for this element and do not know the information of the parent element, the "value" of each key cannot be
         * null. In case no idShort exists, the value must be the list indexer.
         *
         * @param referenceChain The reference pointing to the specific AAS element.
         * @return the builder
         */
        public Builder referenceChain(Reference referenceChain) {
            if (!validate(referenceChain)) {
                throw new IllegalArgumentException("AasDataAddress.Builder received malformed reference");
            }
            try {
                this.property(REFERENCE_CHAIN, JSON_SERIALIZER.write(referenceChain));
            } catch (SerializationException e) {
                throw new EdcException(e);
            }
            return this;
        }

        public Builder copyFrom(DataAddress other) {
            Optional.ofNullable(other)
                    .map(DataAddress::getProperties)
                    .orElse(emptyMap())
                    .forEach((k, v) -> this.property(k, String.valueOf(v)));
            return this;
        }

        @Override
        public AasDataAddress build() {
            this.type(AAS_DATA_TYPE);
            return address;
        }
    }

}
