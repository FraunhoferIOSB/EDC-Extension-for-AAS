package de.fraunhofer.iosb.app.model.aas;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect
public class CustomConceptDescription extends IdsAssetElement {

    protected Identifier identification;
    protected String idShort;

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

        final CustomConceptDescription other = (CustomConceptDescription) obj;

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
