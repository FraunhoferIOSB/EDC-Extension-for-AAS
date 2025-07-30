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
package de.fraunhofer.iosb.ilt.client.policy;

import org.eclipse.edc.spi.system.configuration.Config;

/**
 * Simple {@link org.eclipse.edc.spi.system.configuration.Config} wrapper
 */
public class PolicyServiceConfig {

    private static final boolean ACCEPT_ALL_POLICY_DEFINITIONS_DEFAULT = true;
    private static final int WAIT_FOR_CATALOG_TIMEOUT_DEFAULT = 20;
    private static final String ACCEPTED_POLICY_DEFINITIONS_PATH_DEFAULT = null;

    private final Config config;

    public PolicyServiceConfig(Config config) {
        this.config = config;
    }

    boolean isAcceptAllProviderOffers() {
        return config.getBoolean("acceptAllProviderOffers", ACCEPT_ALL_POLICY_DEFINITIONS_DEFAULT);
    }

    int getWaitForCatalogTimeout() {
        return config.getInteger("waitForCatalogTimeout", WAIT_FOR_CATALOG_TIMEOUT_DEFAULT);
    }

    String getAcceptedPolicyDefinitionsPath() {
        return config.getString("acceptedPolicyDefinitionsPath",
                ACCEPTED_POLICY_DEFINITIONS_PATH_DEFAULT);
    }

}
