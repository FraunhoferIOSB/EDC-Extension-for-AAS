package de.fraunhofer.iosb.app.testutils;

import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class TrustSelfSignedOkHttpClient extends OkHttpClient {

    private TrustManager[] trustAllCerts;
    private SSLContext sslContext;

    private TrustManager[] getTrustManager() throws NoSuchAlgorithmException, KeyManagementException {
        return new TrustManager[] { new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        } };
    }

    private SSLContext getSslContext() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        return sslContext;
    }

    @NotNull
    @Override
    public Builder newBuilder() {
        try {
            trustAllCerts = getTrustManager();
            sslContext = getSslContext();

        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
        return super.newBuilder().sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0]).hostnameVerifier((hostname, session) -> true);
    }
}
