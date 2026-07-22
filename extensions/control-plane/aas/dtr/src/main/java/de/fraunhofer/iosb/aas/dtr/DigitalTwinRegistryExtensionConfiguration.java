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
package de.fraunhofer.iosb.aas.dtr;

import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;

import javax.annotation.Nullable;


@Settings
public class DigitalTwinRegistryExtensionConfiguration {

    private static final String CONTEXT = "edc.aas.dtr";
    private static final String DTR = "Digital Twin Registry ";
    private static final String ACCESS_POLICY = "Access Policy ";
    private static final String USAGE_POLICY = "Usage Policy ";
    private static final String DEFINITION = "Definition ";

    private static final String ID = "ID";
    private static final String PATH = "Path";

    @Setting(description = DTR + " URL", key = CONTEXT + ".url")
    private String url;

    @Setting(description = DTR + " AAS Specification Version", key = CONTEXT
            + ".specification.version", required = false, defaultValue = "3.1")
    private String specificationVersion;

    @Setting(description = DTR + ACCESS_POLICY + DEFINITION + ID
            + ". Preferred over path", key = CONTEXT + ".accessPolicyDefinitionId", required = false)
    private String accessPolicyDefinitionId;

    @Setting(description = DTR + ACCESS_POLICY + PATH, key = CONTEXT + ".accessPolicyPath", required = false)
    private String accessPolicyPath;

    @Setting(description = DTR + USAGE_POLICY + DEFINITION + ID
            + ". Preferred over path", key = CONTEXT + ".usagePolicyDefinitionId", required = false)
    private String usagePolicyDefinitionId;

    @Setting(description = DTR + USAGE_POLICY + PATH, key = CONTEXT + ".usagePolicyPath", required = false)
    private String usagePolicyPath;

    public String getUrl() {
        return url;
    }


    public String getSpecificationVersion() {
        return specificationVersion;
    }


    public @Nullable String getAccessPolicyDefinitionId() {
        return accessPolicyDefinitionId;
    }


    public @Nullable String getAccessPolicyPath() {
        return accessPolicyPath;
    }


    public @Nullable String getUsagePolicyDefinitionId() {
        return usagePolicyDefinitionId;
    }


    public @Nullable String getUsagePolicyPath() {
        return usagePolicyPath;
    }
}
