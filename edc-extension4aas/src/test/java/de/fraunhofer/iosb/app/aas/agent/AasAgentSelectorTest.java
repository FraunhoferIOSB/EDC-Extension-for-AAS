package de.fraunhofer.iosb.app.aas.agent;

import de.fraunhofer.iosb.aas.AasDataProcessorFactory;
import de.fraunhofer.iosb.aas.impl.AllAasDataProcessorFactory;
import de.fraunhofer.iosb.app.aas.agent.impl.RegistryAgent;
import de.fraunhofer.iosb.app.aas.agent.impl.ServiceAgent;
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository;
import de.fraunhofer.iosb.registry.AasServiceRegistry;
import de.fraunhofer.iosb.ssl.SelfSignedCertificateRetriever;
import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AasAgentSelectorTest {

    private AasAgentSelector testSubject;
    private final URL accessUrl = new URL("http://localhost:9999");

    AasAgentSelectorTest() throws MalformedURLException {
    }


    @BeforeEach
    void setUp() {
    }

    @Test
    void testApply() {

    }

    @SuppressWarnings("unchecked")
    @Test
    void testNoAgents() {
        testSubject = new AasAgentSelector(Collections.EMPTY_LIST);
        var result = testSubject.executeSingle(new SelfDescriptionRepository.SelfDescriptionMetaInformation(accessUrl,
                SelfDescriptionRepository.SelfDescriptionSourceType.SERVICE));

        assertTrue(result.failed());
        assertEquals("No AAS agent for type %s found".formatted(SelfDescriptionRepository.SelfDescriptionSourceType.SERVICE), result.getFailureMessages().get(0));
    }

    @Test
    void testWrongAgent() {
        testSubject = new AasAgentSelector(List.of(new RegistryAgent(mock(AasDataProcessorFactory.class), mock(AasServiceRegistry.class))));

        var result = testSubject.executeSingle(new SelfDescriptionRepository.SelfDescriptionMetaInformation(accessUrl,
                SelfDescriptionRepository.SelfDescriptionSourceType.SERVICE));

        assertTrue(result.failed());
        assertEquals("No AAS agent for type %s found".formatted(SelfDescriptionRepository.SelfDescriptionSourceType.SERVICE), result.getFailureMessages().get(0));
    }

    @Test
    void testCorrectAgent() {
        testSubject = new AasAgentSelector(List.of(new ServiceAgent(mockAasDataProcessorFactory())));

        var result = testSubject.executeSingle(new SelfDescriptionRepository.SelfDescriptionMetaInformation(accessUrl,
                SelfDescriptionRepository.SelfDescriptionSourceType.SERVICE));

        assertTrue(result.failed());
        assertEquals("Failed to connect to localhost/[0:0:0:0:0:0:0:1]:9999",
                result.getFailureMessages().get(0));
    }

    private AasDataProcessorFactory mockAasDataProcessorFactory() {
        return new AllAasDataProcessorFactory(mock(SelfSignedCertificateRetriever.class), new OkHttpClient(),
                RetryPolicy.ofDefaults(), new ConsoleMonitor());
    }
}