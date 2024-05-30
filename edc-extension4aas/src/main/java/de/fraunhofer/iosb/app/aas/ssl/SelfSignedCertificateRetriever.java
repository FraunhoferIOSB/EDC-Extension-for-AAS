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
package de.fraunhofer.iosb.app.aas.ssl;

import org.eclipse.edc.spi.EdcException;

import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import static java.lang.String.format;


/**
 * Retrieve certificates of an online service by its URL.
 * This should only be used for explicitly known services and URLs!
 * (Example: Create a FA³ST service which uses TLS with a self-signed certificate if no other is provided.
 * -> Need its cert to communicate with it)
 */
public class SelfSignedCertificateRetriever {


    private static final TrustManager[] TRUST_ALL_MANAGER = new TrustManager[] {
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
    };

    private SelfSignedCertificateRetriever() {
    }

    public static Certificate[] getSelfSignedCertificate(URL url) throws IOException {
        SSLContext sslContext;
        // TLS can have ports other than 443 -> use it
        String proto = url.getPort() == 443 ? "SSL" : "TLS";
        try {
            sslContext = SSLContext.getInstance(proto);
            sslContext.init(null, TRUST_ALL_MANAGER, new SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException generalSecurityException) {
            throw new EdcException(format("Could not retrieve certificates for %s", url),
                    generalSecurityException);
        }

        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        var conn = (HttpsURLConnection) url.openConnection();
        conn.connect();

        var certs = conn.getServerCertificates();
        conn.disconnect();
        return certs;
    }

}
