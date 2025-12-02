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

    Set<String> DEFAULT_EXPOSED_FIELDS_CATALOG = Set.of(
            "AbstractLangString/language",
            "AbstractLangString/text",
            "HasSemantics/semanticId", "Reference/keys", "Reference/type", "Key/type", "Key/value",
            "Referable/idShort", "Referable/description", "Referable/displayName",
            "Identifiable/id"
    );
    Set<String> DEFAULT_EXPOSED_FIELDS_SELF_DESCRIPTION = Set.of(
            "Environment/assetAdministrationShells", "Environment/submodels", "Environment/conceptDescriptions",
            "assetAdministrationShells", "submodels", "conceptDescriptions",
            "AbstractLangString/language",
            "AbstractLangString/text",
            "HasSemantics/semanticId", "Reference/keys", "Reference/type", "Key/type", "Key/value",
            "Referable/idShort", "Referable/description", "Referable/displayName",
            "Identifiable/id",
            // Containers
            "Submodel/submodelElements", "SubmodelElementCollection/value", "SubmodelElementList/value",
            // Display asset ID in self-description
            "HasExtensions/extensions", "Extension/name", "Extension/value"
    );

}
