package de.fraunhofer.iosb.aas.test;

import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.edc.http.client.EdcHttpClientImpl;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;


public class DefaultEdcHttpClient extends EdcHttpClientImpl {
    public DefaultEdcHttpClient() {
        super(new OkHttpClient(), RetryPolicy.ofDefaults(), new ConsoleMonitor());
    }
}
