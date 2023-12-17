package de.fraunhofer.iosb.app.model.aas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.adminshell.aas.v3.model.Key;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonAutoDetect
@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonInclude(Include.NON_NULL)
public class CustomSemanticId {
    private List<CustomSemanticIdKey> keys;

    public List<CustomSemanticIdKey> getKeys() {
        return keys;
    }

    public void setKeys(List<CustomSemanticIdKey> keys) {
        this.keys = keys;
    }

    public CustomSemanticId() {
        this.keys = Collections.emptyList();
    }

    public CustomSemanticId(List<Key> keys) {
        this.keys = new ArrayList<>();
        for (Key key : keys) {

            var customSemanticIdKey = new CustomSemanticIdKey();

            customSemanticIdKey.setIdType(key.getIdType());
            customSemanticIdKey.setType(key.getType());
            customSemanticIdKey.setValue(key.getValue());
            
            this.keys.add(customSemanticIdKey);

        }
    }

}
