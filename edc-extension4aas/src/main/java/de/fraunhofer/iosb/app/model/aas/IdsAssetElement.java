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
package de.fraunhofer.iosb.app.model.aas;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Combining EDC and AAS elements by creating an element with both values.
 */
@JsonAutoDetect
public class IdsAssetElement {

    protected String idsContractId;
    protected String idsAssetId;
    @JsonIgnore // Do not print this in selfDescriptions
    protected String sourceUrl;

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
    
    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

}
