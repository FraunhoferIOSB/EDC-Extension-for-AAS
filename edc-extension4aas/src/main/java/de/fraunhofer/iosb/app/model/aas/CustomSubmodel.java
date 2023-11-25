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

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect
public class CustomSubmodel extends AASElement {

    protected Identifier identification;
    protected String idShort;
    protected List<CustomSubmodelElement> submodelElements = new ArrayList<>();

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

    public List<CustomSubmodelElement> getSubmodelElements() {
        return submodelElements;
    }

    public void setSubmodelElements(List<CustomSubmodelElement> submodelElements) {
        this.submodelElements = submodelElements;
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
