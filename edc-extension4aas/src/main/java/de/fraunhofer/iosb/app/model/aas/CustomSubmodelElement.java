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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.adminshell.aas.v3.model.Constraint;

import java.util.List;
import java.util.Objects;

@JsonAutoDetect
@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonInclude(Include.NON_NULL)
@JsonSubTypes.Type(CustomSubmodelElementCollection.class)
public class CustomSubmodelElement extends AASElement {

    protected List<Constraint> qualifiers;
    protected String idShort;

    public List<Constraint> getQualifiers() {
        return qualifiers;
    }

    public void setQualifiers(List<Constraint> qualifiers) {
        this.qualifiers = qualifiers;
    }

    public String getIdShort() {
        return idShort;
    }

    public void setIdShort(String idShort) {
        this.idShort = idShort;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idShort);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj.getClass() != this.getClass()) {
            return false;
        }

        final CustomSubmodelElement other = (CustomSubmodelElement) obj;

        if (!this.getIdShort().equals(other.getIdShort())) {
            return false;
        }

        if ((this instanceof CustomSubmodelElementCollection)) {
            if (!(other instanceof CustomSubmodelElementCollection)) {
                return false;
            }

            if (Objects.isNull(((CustomSubmodelElementCollection) other).getValue())) {
                return Objects.isNull(((CustomSubmodelElementCollection) this).getValue());
            }

            if (Objects.isNull(((CustomSubmodelElementCollection) this).getValue())) {
                return false;
            }

            return ((CustomSubmodelElementCollection) other).getValue()
                    .containsAll(((CustomSubmodelElementCollection) this).getValue());
        }

        return true;
    }
}
