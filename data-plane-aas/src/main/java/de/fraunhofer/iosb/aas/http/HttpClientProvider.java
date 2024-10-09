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
package de.fraunhofer.iosb.aas.http;

import okhttp3.OkHttpClient;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

/**
 * Provides OkHttpClient instance allowing communication to a remote service with the given certificate.
 */
public class HttpClientProvider {

    public static final String SSL_PROTOCOL = "TLS";

    /**
     * Creates a new OkHttpClient which allows communication with a server holding the given certificateChain.
     *
     * @param certificateChain A server certificate chain for TLS encrypted communication.
     * @return Result containing either a new OkHttpClient instance or failure reason.
     */
    public static Result<OkHttpClient> clientFor(@NotNull Certificate[] certificateChain) {

        TrustManager[] trustManagers;
        SSLContext sslContext;
        try {
            var tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            // Get a new SSL context for each new http client
            sslContext = SSLContext.getInstance(SSL_PROTOCOL);

            var keyStore = createAndPopulateKeyStore(certificateChain);
            tmf.init(keyStore);
            trustManagers = tmf.getTrustManagers();
        } catch (NoSuchAlgorithmException | KeyStoreException trustManagerException) {
            // Something wrong with the system as TLS or a default algorithm is not found
            return Result.failure("%s trying to build trust manager: %s"
                    .formatted(trustManagerException.getClass().getSimpleName(),
                            trustManagerException.getMessage()));
        }

        // No KeyManager needed: we don't need to authenticate ourselves, only trust "others"
        try {
            sslContext.init(null, trustManagers, null);
        } catch (KeyManagementException keyManagementException) {
            return Result.failure("%s trying to initialize SSL context: %s"
                    .formatted(keyManagementException.getClass().getSimpleName(), keyManagementException.getMessage()));
        }

        return Result.success(new OkHttpClient()
                .newBuilder()
                .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustManagers[0])
                .build());
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
