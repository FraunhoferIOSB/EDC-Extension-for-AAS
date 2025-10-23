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
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.edc.validator.spi.Violation;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress.AAS_DATA_TYPE;
import static org.eclipse.edc.dataaddress.httpdata.spi.HttpDataAddressSchema.BASE_URL;
import static org.eclipse.edc.validator.spi.Violation.violation;

/**
 * Validator for HttpData DataAddress type
 */
public class AasDataDataAddressValidator implements Validator<DataAddress> {

    @Override
    public ValidationResult validate(DataAddress dataAddress) {
        List<Violation> violations = new ArrayList<>();
        if (!(dataAddress instanceof AasDataAddress) || !Objects.equals(AAS_DATA_TYPE, dataAddress.getType())) {
            violations.add(violation("DataAddress of type %s malformed.".formatted(AAS_DATA_TYPE), BASE_URL));
        }
        var baseUrl = dataAddress.getStringProperty(BASE_URL);

        if (null == baseUrl) {
            violations.add(violation("DataAddress of type %s must contain a baseUrl.".formatted(AAS_DATA_TYPE), BASE_URL, baseUrl));
        } else {
            try {
                new URL(baseUrl);
            } catch (MalformedURLException e) {
                violations.add(violation("DataAddress of type %s must contain a valid baseUrl.".formatted(AAS_DATA_TYPE), BASE_URL, baseUrl));
            }
        }

        if (dataAddress instanceof AasDataAddress && null == ((AasDataAddress) dataAddress).getPath()) {
            violations.add(violation("DataAddress of type %s must contain a valid path.".formatted(AAS_DATA_TYPE), BASE_URL, baseUrl));
        }
        return violations.isEmpty() ? ValidationResult.success() : ValidationResult.failure(violations);
    }
}