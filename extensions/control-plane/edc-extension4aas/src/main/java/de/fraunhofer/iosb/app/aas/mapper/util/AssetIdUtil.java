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
package de.fraunhofer.iosb.app.aas.mapper.util;

import de.fraunhofer.iosb.ilt.faaast.service.util.ReferenceHelper;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.util.AasUtils;
import org.eclipse.digitaltwin.aas4j.v3.model.Identifiable;
import org.eclipse.digitaltwin.aas4j.v3.model.Key;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress.validate;


/**
 * Utility class to generate distinct Asset IDs based on AAS elements.
 */
public abstract class AssetIdUtil {

    /**
     * Generates a unique and fixed identifier for a given AAS reference and its location in the network.
     *
     * @param url Location of the AAS element in the network
     * @param reference Location of the AAS element in its environment
     * @return A unique and fixed identifier based on the given arguments
     */
    public static String id(String url, Reference reference) {
        List<String> problems = validate(reference);
        if (!problems.isEmpty()) {
            throw new IllegalStateException(String.format("Malformed reference in AasDataAddress: %s \n problems:\n\t%s", reference,
                    String.join("\n\t- ", problems)));
        }

        Key root = ReferenceHelper.getRoot(reference);

        String path = switch (root.getType()) {
            case SUBMODEL -> "submodels/";
            case ASSET_ADMINISTRATION_SHELL -> "shells/";
            case CONCEPT_DESCRIPTION -> "concept-descriptions/";
            default -> throw new IllegalStateException(String.format("Malformed reference in AasDataAddress: %s", reference));
        };

        path = path.concat(Base64.getUrlEncoder().encodeToString(root.getValue().getBytes(StandardCharsets.UTF_8)));

        if (reference.getKeys().size() > 1) {
            path = path.concat("/submodel-elements/")
                    .concat(ReferenceHelper.toPath(reference));
        }

        return String.valueOf("%s:%s".formatted(url, path).hashCode());
    }


    public static String id(String url, Identifiable identifiable) {
        return id(url, AasUtils.toReference(identifiable));
    }
}
