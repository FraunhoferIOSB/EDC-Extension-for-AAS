package de.fraunhofer.iosb.app.model.aas;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.adminshell.aas.v3.model.KeyElements;
import io.adminshell.aas.v3.model.KeyType;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonAutoDetect
@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonInclude(Include.NON_NULL)
public class CustomSemanticIdKey {
    private KeyType idType;
    private KeyElements type;
    private String value;


    public KeyType getIdType() {
        return idType;
    }

    public void setIdType(KeyType idType) {
        this.idType = idType;
    }

    public KeyElements getType() {
        return type;
    }

    public void setType(KeyElements type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
