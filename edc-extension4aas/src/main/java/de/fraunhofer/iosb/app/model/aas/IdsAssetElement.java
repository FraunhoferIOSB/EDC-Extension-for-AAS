package de.fraunhofer.iosb.app.model.aas;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * Combining EDC and AAS elements by creating an element with both values.
 */
@JsonAutoDetect
public class IdsAssetElement {

    protected String idsContractId;
    protected String idsAssetId;

    public String getIdsContractId() {
        return idsContractId;
    }

    public void setIdsContractId(String idsContractId) {
        this.idsContractId = idsContractId;
    }

    public String getIdsAssetId() {
        return idsAssetId;
    }

    public void setIdsAssetId(String idsAssetId) {
        this.idsAssetId = idsAssetId;
    }
}
