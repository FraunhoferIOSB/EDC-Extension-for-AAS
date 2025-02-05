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
package de.fraunhofer.iosb.client.policy;

import org.eclipse.edc.spi.system.configuration.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PolicyServiceConfigTest {
    private PolicyServiceConfig policyServiceConfig;
    private Config config;

    @BeforeEach
    public void initialize() {
        config = mock(Config.class);
        policyServiceConfig = new PolicyServiceConfig(config);
    }

    @Test
    public void getWaitForCatalogTimeoutTest() {
        var expected = 42;
        when(config.getInteger("waitForCatalogTimeout", 20)).thenReturn(expected);

        assertEquals(expected, policyServiceConfig.getWaitForCatalogTimeout());
    }

    @Test
    public void getAcceptedPolicyDefinitionsPathTest() {
        var expected = "/tmp/test/policy-definitions/accepted/";
        when(config.getString("acceptedPolicyDefinitionsPath", null)).thenReturn(expected);

        assertEquals(expected, policyServiceConfig.getAcceptedPolicyDefinitionsPath());
    }

    @Test
    public void isAcceptAllProviderOffersTest() {
        var expected = true;
        when(config.getBoolean("acceptAllProviderOffers", true)).thenReturn(expected);

        assertTrue(policyServiceConfig.isAcceptAllProviderOffers());
    }
}
