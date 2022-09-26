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
package de.fraunhofer.iosb.app.model.ids;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.app.Logger;
import de.fraunhofer.iosb.app.model.aas.CustomAssetAdministrationShell;
import de.fraunhofer.iosb.app.model.aas.CustomAssetAdministrationShellEnvironment;
import de.fraunhofer.iosb.app.model.aas.CustomConceptDescription;
import de.fraunhofer.iosb.app.model.aas.CustomSubmodel;
import de.fraunhofer.iosb.app.model.aas.CustomSubmodelElement;
import de.fraunhofer.iosb.app.util.Encoder;

import java.util.ArrayList;

/**
 * Self description describing the structure of an AAS model
 */
public class SelfDescription {

    private CustomAssetAdministrationShellEnvironment aasEnv; // enriched with contractIds
    private ObjectMapper objectMapper;

    public SelfDescription(CustomAssetAdministrationShellEnvironment newSelfDescription) {
        this.aasEnv = newSelfDescription;
        this.objectMapper = new ObjectMapper();
    }

    public String toString() {
        String aasEnvString;
        try {
            aasEnvString = objectMapper.writeValueAsString(aasEnv);
        } catch (JsonProcessingException e) {
            Logger.getInstance().error("Could not serialize self description", e);
            return null;
        }
        return aasEnvString;
    }

    /**
     * Update self description model

     * @param newShell new element
     */
    public void putShell(CustomAssetAdministrationShell newShell) {
        var updatedAssetAdministrationShells = new ArrayList<>(aasEnv.getAssetAdministrationShells());
        updatedAssetAdministrationShells.add(newShell);
        this.aasEnv.setAssetAdministrationShells(updatedAssetAdministrationShells);
    }

    /**
     * Update self description model

     * @param newSubmodel new element
     */
    public void putSubmodel(CustomSubmodel newSubmodel) {
        var updatedSubmodels = new ArrayList<>(aasEnv.getSubmodels());
        updatedSubmodels.add(newSubmodel);
        this.aasEnv.setSubmodels(updatedSubmodels);
    }

    /**
     * Update self description model

     * @param enclosingSubmodelIdB64 submodel in which this submodel element resides
     * @param newSubmodelElement     new element
     */
    public void putSubmodelElement(String enclosingSubmodelIdB64, CustomSubmodelElement newSubmodelElement) {
        var updatedSubmodels = new ArrayList<>(aasEnv.getSubmodels());
        CustomSubmodel updatedSubmodel = updatedSubmodels.stream()
                .filter(submodel -> Encoder.encodeBase64(submodel.getIdentification().getId())
                        .equals(enclosingSubmodelIdB64))
                .findFirst().orElseThrow();

        var updatedSubmodelElements = new ArrayList<>(updatedSubmodel.getSubmodelElements());
        updatedSubmodelElements.add(newSubmodelElement);
        updatedSubmodel.setSubmodelElements(updatedSubmodelElements);

        updatedSubmodels.set(updatedSubmodels.indexOf(updatedSubmodel), updatedSubmodel);
    }

    public void putConceptDescription(CustomConceptDescription newConceptDescription) {
        var updatedConceptDescriptions = new ArrayList<>(aasEnv.getConceptDescriptions());
        updatedConceptDescriptions.add(newConceptDescription);
        this.aasEnv.setConceptDescriptions(updatedConceptDescriptions);
    }

    public CustomAssetAdministrationShellEnvironment getEnvironment() {
        return aasEnv;
    }

    public void setEnvironment(CustomAssetAdministrationShellEnvironment newSelfDescription) {
        this.aasEnv = newSelfDescription;
    }
}
