package de.fraunhofer.iosb.app.model.ids;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import de.fraunhofer.iosb.app.model.aas.CustomAssetAdministrationShellEnvironment;

public class SelfDescriptionTest {

    private SelfDescription selfDescription;

    @Test
    public void emptyEnvironmentTest() {
        selfDescription = new SelfDescription(new CustomAssetAdministrationShellEnvironment());
        assertEquals("{\"assetAdministrationShells\":null,\"submodels\":null,\"conceptDescriptions\":null}",
                selfDescription.toString());
    }
}
