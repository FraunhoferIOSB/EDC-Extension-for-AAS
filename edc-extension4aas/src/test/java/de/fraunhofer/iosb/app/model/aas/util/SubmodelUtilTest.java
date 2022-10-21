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
