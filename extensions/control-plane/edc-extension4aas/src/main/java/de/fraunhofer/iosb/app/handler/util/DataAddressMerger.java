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
package de.fraunhofer.iosb.app.handler.util;

import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Map;


/**
 * Merges {@link DataAddress} properties, letting binding-provided properties override the base ones on key conflict.
 */
public final class DataAddressMerger {

    private DataAddressMerger() {
        throw new UnsupportedOperationException("Utility class");
    }


    /**
     * Returns a new {@link DataAddress} carrying all properties of the base address, with the given override properties
     * applied on top (override wins on key conflict). The address {@code type} is preserved from the base address. If the
     * override map is null or empty, the base address is returned unchanged.
     *
     * @param base The base data address.
     * @param override Properties to apply on top of the base (nullable).
     * @return The merged data address.
     */
    public static DataAddress merge(DataAddress base, Map<String, String> override) {
        if (override == null || override.isEmpty()) {
            return base;
        }
        var builder = DataAddress.Builder.newInstance().type(base.getType());
        base.getProperties().forEach((key, value) -> builder.property(key, String.valueOf(value)));
        override.forEach(builder::property);
        return builder.build();
    }
}
