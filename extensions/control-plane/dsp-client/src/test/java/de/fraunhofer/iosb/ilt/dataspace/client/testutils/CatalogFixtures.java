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
package de.fraunhofer.iosb.ilt.dataspace.client.testutils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;


/**
 * Loads the {@code catalog.json} test fixture and derives JSON-LD variants of it: catalogs whose offer policy carries
 * no {@code odrl:assigner} (the negotiation flow cannot resolve a counter-party id for such offers), catalogs listing
 * the dataset twice, and catalogs without any dataset.
 */
public final class CatalogFixtures {

    private static final String CATALOG_RESOURCE = "catalog.json";
    private static final String DATASET_KEY = "dcat:dataset";
    private static final String POLICY_KEY = "odrl:hasPolicy";
    private static final String ASSIGNER_KEY = "odrl:assigner";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CatalogFixtures() {}


    /**
     * Returns the bytes of the {@code catalog.json} fixture, which contains one dataset with an acceptable offer.
     */
    public static byte[] catalogBytes() {
        return resourceBytes(CATALOG_RESOURCE);
    }


    /**
     * Returns the bytes of a catalog whose offer policy carries no {@code odrl:assigner}.
     */
    public static byte[] catalogWithoutAssignerBytes() {
        var catalog = readCatalog();
        var policy = (ObjectNode) catalog.path(DATASET_KEY).path(POLICY_KEY);
        policy.remove(ASSIGNER_KEY);
        return writeBytes(catalog);
    }


    /**
     * Returns the bytes of a catalog that lists the fixture's dataset twice, which makes the dataset lookup ambiguous.
     */
    public static byte[] catalogWithDuplicateDatasetsBytes() {
        var catalog = readCatalog();
        var dataset = catalog.path(DATASET_KEY);
        var datasets = (ArrayNode) catalog.putArray(DATASET_KEY);
        datasets.add(dataset.deepCopy()).add(dataset.deepCopy());
        return writeBytes(catalog);
    }


    /**
     * Returns the bytes of a catalog without any dataset.
     */
    public static byte[] catalogWithoutDatasetsBytes() {
        var catalog = readCatalog();
        catalog.putArray(DATASET_KEY);
        return writeBytes(catalog);
    }


    private static ObjectNode readCatalog() {
        try {
            return (ObjectNode) MAPPER.readTree(catalogBytes());
        }
        catch (IOException e) {
            throw new UncheckedIOException("Failed reading " + CATALOG_RESOURCE, e);
        }
    }


    private static byte[] writeBytes(ObjectNode catalog) {
        try {
            return MAPPER.writeValueAsBytes(catalog);
        }
        catch (IOException e) {
            throw new UncheckedIOException("Failed writing catalog variant", e);
        }
    }


    private static byte[] resourceBytes(String name) {
        try (var stream = CatalogFixtures.class.getClassLoader().getResourceAsStream(name)) {
            return Objects.requireNonNull(stream, "missing test resource " + name).readAllBytes();
        }
        catch (IOException e) {
            throw new UncheckedIOException("Failed reading test resource " + name, e);
        }
    }
}
