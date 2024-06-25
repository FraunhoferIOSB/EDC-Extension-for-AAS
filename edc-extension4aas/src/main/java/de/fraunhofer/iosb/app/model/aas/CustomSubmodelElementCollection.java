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

import java.util.Collection;

@JsonAutoDetect
public class CustomSubmodelElementCollection extends CustomSubmodelElement {

    private Collection<CustomSubmodelElement> value;

    public CustomSubmodelElementCollection(String idShort, Collection<CustomSubmodelElement> value) {
        this.idShort = idShort;
        this.value = value;
    }

    public Collection<CustomSubmodelElement> getValue() {
        return value;
    }

    public void setValue(Collection<CustomSubmodelElement> value) {
        this.value = value;
    }

}
