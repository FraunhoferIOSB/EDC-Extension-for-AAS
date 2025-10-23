package de.fraunhofer.iosb.edc.remote;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

@ExtendWith(DependencyInjectionExtension.class)
class EdcConnectorClientExtensionTest {

    private static final EmbeddedRuntime RUNTIME = new EmbeddedRuntime("edc-connector-client-initialize-test-runtime",
            "validator:validator-data-address-aas-data",
            "asset-spi",
            "data-plane-aas-spi",
            "http-spi",
            "policy-spi",
            "contract-spi",
            "transform-lib",
            "control-plane-transform",
            "json-ld",
            "runtime-core",
            "connector-core",
            "edc-connector-client"
    )
            .configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                    "edc.controlplane.management.url", "http://localhost:1"
            )));

    @RegisterExtension
    static RuntimePerClassExtension runtime = new RuntimePerClassExtension(RUNTIME);

    @Test
    void initialize() {
        assert true;
    }
}
