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
package de.fraunhofer.iosb.validator.dataaddress.aasdata;

import de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.ReferenceTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultKey;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress.AAS_DATA_TYPE;
import static org.eclipse.edc.dataaddress.httpdata.spi.HttpDataAddressSchema.BASE_URL;
import static org.junit.jupiter.api.Assertions.assertTrue;


class AasDataDataAddressValidatorTest {

    private final AasDataDataAddressValidator validator = new AasDataDataAddressValidator();


    @Test
    void shouldPass_whenAasDataIsValid() {
        var dataAddress = AasDataAddress.Builder.newInstance()
                .type(AAS_DATA_TYPE)
                .baseUrl("http://this.is/valid/url")
                .reference(new DefaultReference.Builder()
                        .type(ReferenceTypes.MODEL_REFERENCE)
                        .keys(new DefaultKey.Builder()
                                .type(KeyTypes.SUBMODEL)
                                .value(UUID.randomUUID().toString())
                                .build())
                        .build())
                .path("path")
                .build();

        var result = validator.validate(dataAddress);

        assertTrue(result.succeeded());
    }


    @Test
    void shouldFail_whenAasDataBaseUriNotValid() {
        var dataAddress = DataAddress.Builder.newInstance()
                .type(AAS_DATA_TYPE)
                .property(BASE_URL, "not-a-valid-url")
                .build();

        var result = validator.validate(dataAddress);

        assertTrue(result.failed());
    }


    @Test
    void shouldFail_whenAasDataPathNotValid() {
        var dataAddress = DataAddress.Builder.newInstance()
                .property("type", AAS_DATA_TYPE)
                .property(BASE_URL, "http://this.is/valid/url")
                .build();

        var result = validator.validate(dataAddress);

        assertTrue(result.failed());
    }

}
