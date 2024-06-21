package de.fraunhofer.iosb.aas.http;

import okhttp3.OkHttpClient;
import org.eclipse.edc.spi.EdcException;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;


public class HttpClientProvider {

    public static OkHttpClient clientFor(Certificate[] certificateChain) throws KeyStoreException {
        if (certificateChain == null) {
            return new OkHttpClient();
        }

        var keyStore = createAndPopulateKeyStore(certificateChain);

        TrustManager[] trustManagers;
        SSLContext sslContext;
        try {
            var tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            trustManagers = tmf.getTrustManagers();

            sslContext = SSLContext.getInstance("TLS");

        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            // Something wrong with the system as TLS or a default algorithm is not found
            throw new EdcException("An exception occurred trying to accept a self-signed certificate.",
                    noSuchAlgorithmException);
        }

        // No KeyManager needed: we don't need to authenticate ourselves, only trust "others"
        try {
            sslContext.init(null, trustManagers, null);
        } catch (KeyManagementException keyManagementException) {
            throw new EdcException("Could not set self-signed certificate chain", keyManagementException);
        }

        return new OkHttpClient()
                .newBuilder()
                .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustManagers[0])
                .build();
    }

    private static KeyStore createAndPopulateKeyStore(Certificate[] certs) throws KeyStoreException {
        // Create an empty KeyStore
        var keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

        try {
            keyStore.load(null, null);
        } catch (CertificateException | IOException | NoSuchAlgorithmException keyStoreLoadException) {
            // Can not be thrown since we input null
            throw new EdcException("Could not set self-signed certificate chain", keyStoreLoadException);
        }

        // Add each certificate for each service to the KeyStore

        for (var cert : certs) {
            keyStore.setCertificateEntry(String.valueOf(cert.hashCode()), cert);
        }

        return keyStore;
    }
}
