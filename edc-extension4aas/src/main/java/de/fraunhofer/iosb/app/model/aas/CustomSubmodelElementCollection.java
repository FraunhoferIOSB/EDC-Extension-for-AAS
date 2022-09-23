package de.fraunhofer.iosb.app.model.aas;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.ArrayList;
import java.util.Collection;

@JsonAutoDetect
public class CustomSubmodelElementCollection extends CustomSubmodelElement {
    
    private Collection<CustomSubmodelElement> value = new ArrayList<>();

    public Collection<CustomSubmodelElement> getValue() {
        return value;
    }

    public void setValues(Collection<CustomSubmodelElement> value) {
        this.value = value;
    }

}
