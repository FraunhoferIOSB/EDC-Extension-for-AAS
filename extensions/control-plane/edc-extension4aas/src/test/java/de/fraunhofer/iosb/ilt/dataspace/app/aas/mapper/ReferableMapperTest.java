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
package de.fraunhofer.iosb.ilt.dataspace.app.aas.mapper;

import de.fraunhofer.iosb.ilt.dataspace.aas.test.defaults.DefaultVault;
import de.fraunhofer.iosb.ilt.dataspace.app.aas.mapper.referable.identifiable.IdentifiableMapper;
import de.fraunhofer.iosb.ilt.dataspace.app.model.configuration.Configuration;
import de.fraunhofer.iosb.ilt.dataspace.client.repository.remote.impl.RemoteAasRepositoryClient;
import de.fraunhofer.iosb.ilt.dataspace.model.context.repository.remote.RemoteAasRepositoryContext;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.URI;

import static de.fraunhofer.iosb.ilt.dataspace.app.testutils.AasCreator.getSubmodel;
import static de.fraunhofer.iosb.ilt.dataspace.constants.AasConstants.AAS_V31_NAMESPACE;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


class ReferableMapperTest {

    private final IdentifiableMapper testSubject = new IdentifiableMapper(new RemoteAasRepositoryClient(
            new DefaultVault(), new RemoteAasRepositoryContext.Builder().uri(URI.create("http://invalid.local")).build()));


    private static void resetExposedFields() throws Exception {
        // Configuration is a singleton; setExposedFields(null) falls back to the defaults instead of null, so reset
        // the underlying field to its unconfigured (null) state to make the "defaults" case deterministic.
        Field field = Configuration.class.getDeclaredField("exposedFields");
        field.setAccessible(true);
        field.set(Configuration.getInstance(), null);
    }


    @BeforeEach
    void setUp() throws Exception {
        resetExposedFields();
    }


    @AfterEach
    void tearDown() throws Exception {
        resetExposedFields();
    }


    @Test
    void map_withDefaultExposedFields_shouldContainDefaultFields() {
        Submodel submodel = getSubmodel();

        Asset asset = testSubject.map(submodel);

        // Fields from DEFAULT_EXPOSED_FIELDS that are populated on the Submodel should be present (namespaced)
        assertNotNull(asset.getProperty(AAS_V31_NAMESPACE.concat("Referable/idShort")));
        assertNotNull(asset.getProperty(AAS_V31_NAMESPACE.concat("Referable/description")));
        assertNotNull(asset.getProperty(AAS_V31_NAMESPACE.concat("Identifiable/id")));
        assertNotNull(asset.getProperty(AAS_V31_NAMESPACE.concat("HasSemantics/semanticId")));

        // modelingType is always added by ReferableMapper, independent of the exposedFields configuration
        assertNotNull(asset.getProperty(AAS_V31_NAMESPACE.concat("modelingType")));

        // Fields not contained in DEFAULT_EXPOSED_FIELDS must not leak into the asset
        assertNull(asset.getProperty(AAS_V31_NAMESPACE.concat("Submodel/submodelElements")));
        assertNull(asset.getProperty(AAS_V31_NAMESPACE.concat("Referable/category")));
        assertNull(asset.getProperty(AAS_V31_NAMESPACE.concat("HasKind/kind")));
    }


    @Test
    void map_withConfiguredExposedFields_shouldContainOnlyConfiguredFields() {
        // Override the defaults with a single exposed field
        Configuration.getInstance().setExposedFields("Referable/idShort");

        Submodel submodel = getSubmodel();

        Asset asset = testSubject.map(submodel);

        // The configured field is present
        assertNotNull(asset.getProperty(AAS_V31_NAMESPACE.concat("Referable/idShort")));

        // modelingType is always added by ReferableMapper, independent of the exposedFields configuration
        assertNotNull(asset.getProperty(AAS_V31_NAMESPACE.concat("modelingType")));

        // Even fields that are part of DEFAULT_EXPOSED_FIELDS must be absent when explicitly configured otherwise
        assertNull(asset.getProperty(AAS_V31_NAMESPACE.concat("Identifiable/id")));
        assertNull(asset.getProperty(AAS_V31_NAMESPACE.concat("HasSemantics/semanticId")));
        assertNull(asset.getProperty(AAS_V31_NAMESPACE.concat("Referable/description")));

        // Non-default fields stay absent too
        assertNull(asset.getProperty(AAS_V31_NAMESPACE.concat("Submodel/submodelElements")));
        assertNull(asset.getProperty(AAS_V31_NAMESPACE.concat("Referable/category")));
    }
}
