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

public interface AasConstants {
    String AAS_PREFIX = "aas";
    String AAS_V30_NAMESPACE = "https://admin-shell.io/aas/3/0/";
    String SUPPORTED_AAS_VERSION = "3.0";

    String EDC_SETTINGS_PREFIX = "edc.aas";

    String DEFAULT_ACCESS_POLICY_DEFINITION_ID = "AAS_DEFAULT_ACCESS_POLICY";
    String DEFAULT_CONTRACT_POLICY_DEFINITION_ID = "AAS_DEFAULT_CONTRACT_POLICY";
}
