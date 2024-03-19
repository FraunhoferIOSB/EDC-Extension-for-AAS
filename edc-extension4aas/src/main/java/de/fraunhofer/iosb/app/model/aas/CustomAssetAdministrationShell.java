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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.adminshell.aas.v3.model.AssetInformation;
import io.adminshell.aas.v3.model.impl.DefaultAssetInformation;

import java.util.Objects;

/**
 * AAS Model for the self description of the edc
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonAutoDetect
public class CustomAssetAdministrationShell extends AASElement {

    protected Identifier identification;
    protected String idShort;
    protected AssetInformation assetInformation;

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

    public AssetInformation getAssetInformation() {
        return assetInformation;
    }

    public void setAssetInformation(DefaultAssetInformation assetInformation) {
        this.assetInformation = assetInformation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(identification, idShort);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj.getClass() != this.getClass()) {
            return false;
        }

        final CustomAssetAdministrationShell other = (CustomAssetAdministrationShell) obj;

        if ((this.getIdentification() == null) ? (other.getIdentification() != null)
                : !this.getIdentification().equals(other.getIdentification())) {
            return false;
        }

        return this.getIdShort().equals(other.getIdShort());
    }
}