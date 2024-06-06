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
package de.fraunhofer.iosb.app.dataplane.aas.pipeline;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import de.fraunhofer.iosb.app.util.Encoder;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Map;
import java.util.Objects;

import static de.fraunhofer.iosb.app.dataplane.aas.pipeline.AasDataSourceFactory.AAS_DATA_TYPE;
import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static org.eclipse.edc.dataaddress.httpdata.spi.HttpDataAddressSchema.BASE_URL;

/**
 * Inspired by {@link org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress}
 * Enables more specific communication with AAS services
 */
@JsonTypeName()
@JsonDeserialize(builder = DataAddress.Builder.class)
public class AasDataAddress extends DataAddress {

    public static final String REFERENCE_CHAIN = "referenceChain";

    private static final String METHOD = "method";
    private static final String ADDITIONAL_HEADER = "header";
    private static final String QUERY_PARAMS = "queryParams";


    private AasDataAddress() {
        super();
        this.setType(AAS_DATA_TYPE);
    }

    @JsonIgnore
    public String getBaseUrl() {
        return getStringProperty(BASE_URL);
    }

    public Map<String, String> getAdditionalHeaders() {
        return getProperties().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(ADDITIONAL_HEADER))
                .collect(toMap(entry -> entry.getKey().replace(ADDITIONAL_HEADER, ""), it -> (String) it.getValue()));
    }


    public Reference getReferenceChain() {
        var referenceChain = properties.get(REFERENCE_CHAIN);

        if (Objects.isNull(referenceChain)) {
            return new DefaultReference();
        }
        if (referenceChain instanceof Reference) {
            return (Reference) referenceChain;
        }
        throw new EdcException(new IllegalStateException("Something not of type Reference was stored in the property of an AasDataAddress!"));
    }

    public String referenceChainAsPath() {
        StringBuilder urlBuilder = new StringBuilder();

        for (var key : getReferenceChain().getKeys()) {

            switch (key.getType()) {
                case ASSET_ADMINISTRATION_SHELL:
                    urlBuilder.append("/shells/").append(Encoder.encodeBase64(key.getValue()));
                    break;
                case SUBMODEL:
                    urlBuilder.append("/submodels/").append(Encoder.encodeBase64(key.getValue()));
                    break;
                case CONCEPT_DESCRIPTION:
                    urlBuilder.append("/concept-descriptions/").append(Encoder.encodeBase64(key.getValue()));
                    break;
                case SUBMODEL_ELEMENT:
                case SUBMODEL_ELEMENT_COLLECTION:
                case SUBMODEL_ELEMENT_LIST:
                    if (urlBuilder.indexOf("/submodel-elements/") == -1) {
                        urlBuilder.append("/submodel-elements/");
                    } else {
                        urlBuilder.append(".");
                    }
                    urlBuilder.append(key.getValue());
                    break;
                default:
                    throw new EdcException(new IllegalStateException(format("Element type not recognized in AasDataAddress: %s", key.getType())));
            }

        }

        return urlBuilder.toString();
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

        public Builder baseUrl(String baseUrl) {
            this.property(BASE_URL, baseUrl);
            return this;
        }

        public Builder method(String method) {
            this.property(METHOD, method);
            return this;
        }


        public Builder referenceChain(Reference referenceChain) {
            Objects.requireNonNull(referenceChain.getKeys());

            this.property(REFERENCE_CHAIN, referenceChain);
            return this;
        }


        @Override
        public AasDataAddress build() {
            this.type(AAS_DATA_TYPE);
            return address;
        }
    }
}
