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
        when(config.getInteger("waitForCatalogTimeout", 10)).thenReturn(expected);

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
        when(config.getBoolean("acceptAllProviderOffers", false)).thenReturn(expected);

        assertTrue(policyServiceConfig.isAcceptAllProviderOffers());
    }
}
