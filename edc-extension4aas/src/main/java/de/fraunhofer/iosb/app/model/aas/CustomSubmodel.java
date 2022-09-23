package de.fraunhofer.iosb.app.model.aas;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect
public class CustomSubmodel extends IdsAssetElement {

    protected Identifier identification;
    protected String idShort;
    protected Collection<CustomSubmodelElement> submodelElements = new ArrayList<>();

    public Identifier getIdentification() {
        return identification;
    }

    public void setIdentification(Identifier identification) {
        this.identification = identification;
    }

    public String getIdShort() {
        return idShort;
    }

    public void setIdShort(String idShort) {
        this.idShort = idShort;
    }

    public Collection<CustomSubmodelElement> getSubmodelElements() {
        return submodelElements;
    }

    public void setSubmodelElements(List<CustomSubmodelElement> submodelElements) {
        this.submodelElements = submodelElements;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj.getClass() != this.getClass()) {
            return false;
        }

        final CustomSubmodel other = (CustomSubmodel) obj;

        if ((this.getIdentification() == null) ? (other.getIdentification() != null)
                : !this.getIdentification().equals(other.getIdentification())) {
            return false;
        }

        if (!this.getIdShort().equals(other.getIdShort())) {
            return false;
        }

        return true;
    }
}
