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
package de.fraunhofer.iosb.app.util;

import de.fraunhofer.iosb.app.model.aas.CustomSubmodel;
import de.fraunhofer.iosb.app.model.aas.CustomSubmodelElementCollection;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodelElementCollection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SubmodelUtilTest {
    private static CustomSubmodel testCustomSubmodel;

    @BeforeAll
    public static void createNestedCustomSubmodelElementCollection() {
        testCustomSubmodel = new CustomSubmodel();
        var incrediblyNestedCustomCollectionWithOneHundredLayers = new CustomSubmodelElementCollection("0", null);
        var customCollObj = incrediblyNestedCustomCollectionWithOneHundredLayers;
        for (int i = 0; i < 100; i++) {
            var newCustomColl = new CustomSubmodelElementCollection(String.valueOf(i), null);
            customCollObj.setValues(List.of(newCustomColl));
            customCollObj = newCustomColl;
        }
        testCustomSubmodel.setSubmodelElements(List.of(incrediblyNestedCustomCollectionWithOneHundredLayers));
    }

    @Test
    public void getAllSubmodelElementsTest() {
        assertEquals(101, AASUtil.getAllSubmodelElements(testCustomSubmodel).size());
    }

    @Test
    public void getCustomSubmodelElementStructureFromSubmodel() {
        var testSubmodel = new DefaultSubmodel();
        var incrediblyNestedCollectionWithOneHundredLayers = new DefaultSubmodelElementCollection();
        var collObj = incrediblyNestedCollectionWithOneHundredLayers;
        collObj.setIdShort("0");

        for (int i = 0; i < 100; i++) {
            var newColl = new DefaultSubmodelElementCollection();
            newColl.setIdShort(String.valueOf(i));
            collObj.setValue(List.of(newColl));
            collObj = newColl;
        }

        testSubmodel.setSubmodelElements(List.of(incrediblyNestedCollectionWithOneHundredLayers));

        // Cast to use equals method
        assertEquals(
                testCustomSubmodel.getSubmodelElements().stream().findFirst().orElseThrow(),
                AASUtil.getCustomSubmodelElementStructureFromSubmodel(testSubmodel)
                        .stream().findFirst().orElseThrow());
    }
}
