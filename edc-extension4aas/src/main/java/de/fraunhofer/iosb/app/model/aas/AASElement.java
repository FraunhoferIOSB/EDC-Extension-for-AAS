package de.fraunhofer.iosb.app.model.aas;

/*
 * Collect common attributes of every AAS element.
 */
public class AASElement extends IdsAssetElement {

    protected String idShort;

    public String getIdShort() {
        return idShort;
    }

    public void setIdShort(String idShort) {
        this.idShort = idShort;
    }
}
