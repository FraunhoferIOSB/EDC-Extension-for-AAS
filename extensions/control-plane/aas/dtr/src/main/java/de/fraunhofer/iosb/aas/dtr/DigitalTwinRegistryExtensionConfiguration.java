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


/**
 * Configuration of the Digital Twin Registry extension, holding the DTR URL, AAS specification version and the
 * access/usage policy definition IDs or paths used to register the DTR as an EDC asset.
 */
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

    @Setting(description = DTR + " AAS Specification Version", key = CONTEXT +
            ".specification.version", required = false, defaultValue = "3.1")
    private String specificationVersion;

    @Setting(description = DTR + ACCESS_POLICY + DEFINITION + ID +
            ". Preferred over path", key = CONTEXT + ".accessPolicyDefinitionId", required = false)
    private String accessPolicyDefinitionId;

    @Setting(description = DTR + ACCESS_POLICY + PATH, key = CONTEXT + ".accessPolicyPath", required = false)
    private String accessPolicyPath;

    @Setting(description = DTR + USAGE_POLICY + DEFINITION + ID +
            ". Preferred over path", key = CONTEXT + ".usagePolicyDefinitionId", required = false)
    private String usagePolicyDefinitionId;

    @Setting(description = DTR + USAGE_POLICY + PATH, key = CONTEXT + ".usagePolicyPath", required = false)
    private String usagePolicyPath;

    /**
     * Creates a new Digital Twin Registry extension configuration; populated by the EDC configuration mechanism.
     */
    public DigitalTwinRegistryExtensionConfiguration() {
    }

    /**
     * Returns the Digital Twin Registry URL.
     *
     * @return the Digital Twin Registry URL.
     */
    public String getUrl() {
        return url;
    }


    /**
     * Returns the AAS specification version of the Digital Twin Registry.
     *
     * @return the AAS specification version.
     */
    public String getSpecificationVersion() {
        return specificationVersion;
    }


    /**
     * Returns the access policy definition ID. Preferred over the access policy path.
     *
     * @return the access policy definition ID, or {@code null} if not configured.
     */
    public String getAccessPolicyDefinitionId() {
        return accessPolicyDefinitionId;
    }


    /**
     * Returns the path to the access policy file.
     *
     * @return the access policy path, or {@code null} if not configured.
     */
    public String getAccessPolicyPath() {
        return accessPolicyPath;
    }


    /**
     * Returns the usage policy definition ID. Preferred over the usage policy path.
     *
     * @return the usage policy definition ID, or {@code null} if not configured.
     */
    public String getUsagePolicyDefinitionId() {
        return usagePolicyDefinitionId;
    }


    /**
     * Returns the path to the usage policy file.
     *
     * @return the usage policy path, or {@code null} if not configured.
     */
    public String getUsagePolicyPath() {
        return usagePolicyPath;
    }
}
