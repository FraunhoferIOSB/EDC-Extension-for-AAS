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
package de.fraunhofer.iosb.constants;

import java.util.Set;


public interface AasConstants {
    String AAS_PREFIX = "aas";
    String AAS_V30_NAMESPACE = "https://admin-shell.io/aas/3/0/";
    String SUPPORTED_AAS_VERSION = "3.0";

    String EDC_SETTINGS_PREFIX = "edc.aas";

    String DEFAULT_POLICY_DEFINITION_ID = "allow_all";

    /**
     * The fields in this set are added as additional metadata in the catalog and self-description, unless configured otherwise with the `edc.aas.exposedFields` configuration
     * variable.
     * <p>
     * Note: In the self-description, some fields are exposed additionally by default. If this is unwanted, the self-description should not be exposed (use edc.aas
     * .exposeSelfDescription=false).
     */
    Set<String> DEFAULT_EXPOSED_FIELDS = Set.of(
            "AbstractLangString/language", "AbstractLangString/text",
            "HasSemantics/semanticId", "Reference/keys", "Reference/type", "Key/type", "Key/value",
            "Referable/idShort", "Referable/description", "Referable/displayName",
            "Identifiable/id"
    );

    /**
     * To achieve an environment-like structured view of the registered elements, additional fields need to be exposed (e.g., submodelElements of a submodel).
     * <p>
     * This may expose an element "in the middle", i.e. if a submodel(ElementCollection/List) is not registered but a child element of it is, it will be exposed in the
     * self-description. If this is not acceptable due to confidentiality reasons, the self-description should not be exposed and the catalog should be used as a discovery
     * mechanism instead.
     */
    Set<String> EXPOSED_FIELDS_SELF_DESCRIPTION = Set.of(
            // Environment top-level elements
            "Environment/assetAdministrationShells", "Environment/submodels", "Environment/conceptDescriptions",
            // Containers
            "Submodel/submodelElements", "SubmodelElementCollection/value", "SubmodelElementList/value",
            // Display asset ID in self-description
            "HasExtensions/extensions", "Extension/name", "Extension/value"
    );

}
