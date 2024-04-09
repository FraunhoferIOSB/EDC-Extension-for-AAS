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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomAssetAdministrationShellEnvironment {

    protected List<CustomAssetAdministrationShell> assetAdministrationShells = new ArrayList<>();

    protected List<CustomSubmodel> submodels = new ArrayList<>();

    protected List<CustomConceptDescription> conceptDescriptions = new ArrayList<>();

    public List<CustomAssetAdministrationShell> getAssetAdministrationShells() {
        return assetAdministrationShells;
    }

    public void setAssetAdministrationShells(List<CustomAssetAdministrationShell> shells) {
        this.assetAdministrationShells = shells;
    }

    public List<CustomSubmodel> getSubmodels() {
        return submodels;
    }

    public void setSubmodels(List<CustomSubmodel> submodels) {
        this.submodels = submodels;
    }

    public List<CustomConceptDescription> getConceptDescriptions() {
        return conceptDescriptions;
    }

    public void setConceptDescriptions(List<CustomConceptDescription> conceptDescriptions) {
        this.conceptDescriptions = conceptDescriptions;
    }

}
