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

import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static org.eclipse.edc.dataaddress.httpdata.spi.HttpDataAddressSchema.BASE_URL;
import static org.eclipse.edc.dataaddress.httpdata.spi.HttpDataAddressSchema.HTTP_DATA_TYPE;
import static org.eclipse.edc.validator.spi.Violation.violation;


/**
 * Validator for HttpData DataAddress type
 */
public class AasDataDataAddressValidator implements Validator<DataAddress> {

    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "PATCH");

    @Override
    public ValidationResult validate(DataAddress dataAddress) {
        var baseUrl = dataAddress.getStringProperty("baseUrl");
        try {
            new URL(baseUrl);
        }
        catch (MalformedURLException e) {
            var violation = violation("DataAddress of type %s must contain a valid baseUrl.".formatted(HTTP_DATA_TYPE), BASE_URL, baseUrl);
            return ValidationResult.failure(violation);
        }

        var method = dataAddress.getStringProperty("method");
        if (method != null && allowedMethods.stream().filter(allowed -> allowed.equalsIgnoreCase(method)).findAny().isEmpty()) {
            var violation = violation("DataAddress of type %s needs a valid HTTP method, if one is provided.".formatted(HTTP_DATA_TYPE), "method", baseUrl);
            return ValidationResult.failure(violation);
        }

        if (method != null && allowedMethods.stream().filter(allowed -> allowed.equalsIgnoreCase(method)).findAny().isEmpty()) {
            var violation = violation("DataAddress of type %s needs a valid HTTP method, if one is provided.".formatted(HTTP_DATA_TYPE), "method", baseUrl);
            return ValidationResult.failure(violation);
        }

        return ValidationResult.success();
    }
}
