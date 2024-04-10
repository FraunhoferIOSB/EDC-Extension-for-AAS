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
import org.eclipse.digitaltwin.aas4j.v3.model.Qualifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect
public class CustomSubmodel extends AssetAdministrationShellElement {

    protected String id;
    protected List<Qualifier> qualifiers;
    protected String idShort;
    protected List<CustomSubmodelElement> submodelElements = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Qualifier> getQualifiers() {
        return qualifiers;
    }

    public void setQualifiers(List<Qualifier> qualifiers) {
        this.qualifiers = qualifiers;
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
    public int hashCode() {
        return Objects.hash(id, idShort);
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

        if ((this.getId() == null) ? (other.getId() != null)
                : !this.getId().equals(other.getId())) {
            return false;
        }

        return Objects.equals(this.getId(), other.getId()) && Objects.equals(this.getIdShort(), other.getIdShort());
    }

}
