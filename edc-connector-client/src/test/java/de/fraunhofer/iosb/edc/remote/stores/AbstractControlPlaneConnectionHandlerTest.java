package de.fraunhofer.iosb.edc.remote.stores;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Body;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import de.fraunhofer.iosb.edc.remote.transform.Codec;
import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.edc.http.client.EdcHttpClientImpl;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Spy;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.not;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public abstract class AbstractControlPlaneConnectionHandlerTest {

    @RegisterExtension
    protected static WireMockExtension server = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();
    protected final EdcHttpClient httpClient = new EdcHttpClientImpl(new OkHttpClient(), RetryPolicy.ofDefaults(), new ConsoleMonitor());
    protected final String apiKey = UUID.randomUUID().toString();
    @Spy
    protected Monitor monitor = spy(new ConsoleMonitor());
    protected Codec mockCodec = mock(Codec.class);

    @AfterAll
    static void tearDownAll() {
        server.shutdownServer();
    }

    @AfterEach
    void tearDown() {
        // Remove mocks;
        mockCodec = mock(Codec.class);
        server.resetAll();
    }

    protected void mockResponseForGet(String path) {
        server.stubFor(WireMock.get(urlPathEqualTo(path))
                .willReturn(aResponse()
                        .withResponseBody(Body.ofBinaryOrText("test-return-body".getBytes(StandardCharsets.UTF_8), ContentTypeHeader.absent()))
                        .withStatus(200)));
    }

    protected void mockResponseForPost(String path) {
        var postMock = WireMock.post(urlPathEqualTo(path))
                .withRequestBody(matching("test-body"))
                .willReturn(aResponse()
                        .withResponseBody(Body.ofBinaryOrText("test-return-body".getBytes(StandardCharsets.UTF_8), ContentTypeHeader.absent()))
                        .withStatus(200));

        server.stubFor(postMock);
    }

    protected void authorizedServer() {
        server.stubFor(any(anyUrl()).withHeader("x-api-key", matching(apiKey)).willReturn(aResponse().withStatus(200)));
        server.stubFor(any(anyUrl()).withHeader("x-api-key", not(matching(apiKey))).willReturn(aResponse().withStatus(403)));
    }

}
