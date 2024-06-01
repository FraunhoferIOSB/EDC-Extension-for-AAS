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
import org.eclipse.edc.spi.types.domain.DataAddress;

import static de.fraunhofer.iosb.app.dataplane.aas.pipeline.AasDataSourceFactory.AAS_DATA_TYPE;
import static org.eclipse.edc.dataaddress.httpdata.spi.HttpDataAddressSchema.BASE_URL;

/**
 * Inspired by {@link org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress}
 * Enables more specific communication with AAS services
 */
@JsonTypeName()
@JsonDeserialize(builder = DataAddress.Builder.class)
public class AasDataAddress extends DataAddress {

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


    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder extends DataAddress.Builder<AasDataAddress, Builder> {

        private Builder() {
            super(new AasDataAddress());
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

        public Builder headers(String key, String value) {
            this.property(ADDITIONAL_HEADER + key, value);
            return this;
        }

        public Builder queryParams(String queryParams) {
            this.property(QUERY_PARAMS, queryParams);
            return this;
        }

        @Override
        public AasDataAddress build() {
            this.type(AAS_DATA_TYPE);
            return address;
        }
    }
}
