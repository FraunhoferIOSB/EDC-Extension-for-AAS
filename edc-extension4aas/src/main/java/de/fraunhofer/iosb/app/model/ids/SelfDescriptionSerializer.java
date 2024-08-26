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
package de.fraunhofer.iosb.app.model.ids;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import java.util.Map;

import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.PROPERTY_ID;

public class SelfDescriptionSerializer {

    private static final String[] SKIPPED_FIELDS = new String[] {"dataAddress", "privateProperties", "createdAt",
            PROPERTY_ID};
    private static final ObjectWriter OBJECT_WRITER = createObjectWriter();

    private SelfDescriptionSerializer() {
        throw new RuntimeException("Utility class");
    }

    /**
     * Serialize Asset into (kind of) IDS SelfDescription form
     *
     * @param asset Asset containing environment to be serialized
     * @return SelfDescription or empty string on exception
     */
    public static String assetToString(Asset asset) {
        try {
            var environmentList = Map.of(
                    "shells", asset.getProperty("shells"),
                    "submodels", asset.getProperty("submodels"),
                    "conceptDescriptions", asset.getProperty("conceptDescriptions"));
            return OBJECT_WRITER.writeValueAsString(environmentList);
        } catch (JsonProcessingException e) {
            return "";
        }
    }

    private static ObjectWriter createObjectWriter() {
        var objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        // Custom serialization rules
        //  var simpleModule = new SimpleModule();
        // simpleModule.addSerializer(Asset.class, new AssetSerializer());
        // objectMapper.registerModule(simpleModule);

        // Set skipped fields
        objectMapper.addMixIn(Asset.class, AssetFilterMixin.class);
        var filter = new SimpleFilterProvider()
                .addFilter("assetFilter", SimpleBeanPropertyFilter.serializeAllExcept(SKIPPED_FIELDS));
        objectMapper.setFilterProvider(filter);

        return objectMapper.writer().with(filter);
    }

    @JsonFilter("assetFilter")
    private abstract static class AssetFilterMixin {
    }
}
