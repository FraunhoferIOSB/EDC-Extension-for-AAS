package de.fraunhofer.iosb.app.model.ids;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

public class SelfDescriptionSerializer {

    private static final String[] skippedFields = new String[]{"dataAddress", "privateProperties", "createdAt"};
    private static final ObjectWriter objectWriter = createObjectWriter();

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
            return objectWriter.writeValueAsString(asset);
        } catch (JsonProcessingException e) {
            return "";
        }
    }

    private static ObjectWriter createObjectWriter() {
        var objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.addMixIn(Asset.class, AssetFilterMixin.class);
        var filter = new SimpleFilterProvider().addFilter("assetFilter", SimpleBeanPropertyFilter.serializeAllExcept(
                skippedFields));
        objectMapper.setFilterProvider(filter);
        return objectMapper.writer().with(filter);
    }

    @JsonFilter("assetFilter")
    private abstract static class AssetFilterMixin {
    }
}
