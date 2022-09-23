package de.fraunhofer.iosb.app.model.aas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomAssetAdministrationShellEnvironment {

    protected List<CustomAssetAdministrationShell> assetAdministrationShells;

    protected List<CustomSubmodel> submodels;

    protected List<CustomConceptDescription> conceptDescriptions;

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
