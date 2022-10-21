package de.fraunhofer.iosb.app.model.aas.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.fraunhofer.iosb.app.model.aas.CustomSubmodel;
import de.fraunhofer.iosb.app.model.aas.CustomSubmodelElement;
import de.fraunhofer.iosb.app.model.aas.CustomSubmodelElementCollection;
import io.adminshell.aas.v3.model.impl.DefaultSubmodel;
import io.adminshell.aas.v3.model.impl.DefaultSubmodelElementCollection;

public class SubmodelUtilTest {
    private static CustomSubmodel testCustomSubmodel;

    @BeforeAll
    public static void createNestedCustomSubmodelElementCollection() {
        testCustomSubmodel = new CustomSubmodel();
        final var incrediblyNestedCustomCollectionWithOneHundredLayers = new CustomSubmodelElementCollection();
        var customCollObj = incrediblyNestedCustomCollectionWithOneHundredLayers;
        customCollObj.setIdShort("0");
        for (int i = 0; i < 100; i++) {
            var newCustomColl = new CustomSubmodelElementCollection();
            newCustomColl.setIdShort("" + i);
            customCollObj.setValues(List.of(newCustomColl));
            customCollObj = newCustomColl;
        }
        testCustomSubmodel.setSubmodelElements(List.of(incrediblyNestedCustomCollectionWithOneHundredLayers));
    }

    @Test
    public void getAllSubmodelElementsTest() {
        assertEquals(101, SubmodelUtil.getAllSubmodelElements(testCustomSubmodel).size());
    }

    @Test
    public void getCustomSubmodelElementStructureFromSubmodel() {
        var testSubmodel = new DefaultSubmodel();
        final var incrediblyNestedCollectionWithOneHundredLayers = new DefaultSubmodelElementCollection();
        var collObj = incrediblyNestedCollectionWithOneHundredLayers;
        collObj.setIdShort("0");

        for (int i = 0; i < 100; i++) {
            var newColl = new DefaultSubmodelElementCollection();
            newColl.setIdShort("" + i);
            collObj.setValues(List.of(newColl));
            collObj = newColl;
        }
        
        testSubmodel.setSubmodelElements(List.of(incrediblyNestedCollectionWithOneHundredLayers));

        // Cast to use equals method
        assertEquals(
                (CustomSubmodelElement) testCustomSubmodel.getSubmodelElements().stream().findFirst().orElseThrow(),
                (CustomSubmodelElement) SubmodelUtil.getCustomSubmodelElementStructureFromSubmodel(testSubmodel)
                        .stream().findFirst().orElseThrow());
    }
}
