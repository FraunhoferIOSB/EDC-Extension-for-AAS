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
package de.fraunhofer.iosb.app.aas;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.ConceptDescription;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.edc.spi.EdcException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;


/**
 * With string input this parser creates AAS model elements such as:
 * CustomAssetAdministrationShell, CustomConceptDescription, CustomSubmodel (with SubmodelElements)
 * In these
 */
public class ModelParser {

    private final JsonDeserializer jsonDeserializer = new JsonDeserializer();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String accessUrl;

    /**
     * Constructor
     *
     * @param url URL used for accessing the AAS service.
     */
    ModelParser(String url) {
        this.accessUrl = url;
    }

    List<ConceptDescription> parseConceptDescriptions(String conceptDescriptions) {
        return Optional.ofNullable(readList(conceptDescriptions, ConceptDescription.class)).orElse(new ArrayList<>());
    }

    List<AssetAdministrationShell> parseShells(String shells) {
        return Optional.ofNullable(readList(shells, AssetAdministrationShell.class)).orElse(new ArrayList<>());
    }

    List<Submodel> parseSubmodels(String submodelsString) {
        return Optional.ofNullable(readList(submodelsString, Submodel.class)).orElse(new ArrayList<>());
    }

    private <T> List<T> readList(String serialized, Class<T> clazz) {
        try {
            var responseJson = objectMapper.readTree(serialized).get("result");
            return jsonDeserializer.readList(responseJson, clazz);
        } catch (JsonProcessingException | DeserializationException e) {
            throw new EdcException(format("Failed parsing list of %s from %s", clazz.getName(), accessUrl), e);
        }
    }

}
