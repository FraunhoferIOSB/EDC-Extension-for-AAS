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
package de.fraunhofer.iosb.ssl.impl;

import de.fraunhofer.iosb.ssl.SelfSignedCertificateRetriever;
import org.eclipse.edc.spi.result.Result;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


/**
 * Retrieve certificates of an online service by its URL. This should only be used for explicitly known services and URLs! (Example: Create a FA³ST service which uses TLS with a
 * self-signed certificate if no other is provided. -> Need its cert to communicate with it)
 */
public class DefaultSelfSignedCertificateRetriever implements SelfSignedCertificateRetriever {

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


    public static boolean isTrusted(String uriString) {
        HttpsURLConnection.setDefaultSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault());
        URI uri;
        try {
            uri = new URI(uriString);
        }
        catch (URISyntaxException e) {
            return false;
        }
        try {
            var conn = (HttpsURLConnection) uri.toURL().openConnection();
            conn.connect();
            // Connection with standard java library succeeded
            // -> according to this system, the server has a trusted certificate
            return true;
        }
        catch (IOException e) {
            return false;
        }
    }


    public Result<Certificate[]> getSelfSignedCertificate(String urlString) {
        SSLContext sslContext;
        URI uri;
        try {
            uri = new URI(urlString);
        }
        catch (URISyntaxException uriSyntaxException) {
            return Result.failure(List.of(uriSyntaxException.getMessage()));
        }

        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, TRUST_ALL_MANAGER, new SecureRandom());
        }
        catch (NoSuchAlgorithmException | KeyManagementException generalSecurityException) {
            return Result.failure(List.of(generalSecurityException.getMessage()));
        }

        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        HttpsURLConnection conn;

        try {
            conn = (HttpsURLConnection) uri.toURL().openConnection();
            conn.connect();
        }
        catch (IOException e) {
            return Result.failure(List.of(e.getMessage()));
        }

        X509Certificate[] certs;
        try {
            certs = (X509Certificate[]) conn.getServerCertificates();
        }
        catch (SSLPeerUnverifiedException e) {
            return Result.failure("peer unverified");
        }

        try {
            for (X509Certificate cert: certs) {
                cert.checkValidity();
            }
        }
        catch (CertificateExpiredException | CertificateNotYetValidException e) {
            return Result.failure("expired");
        }

        conn.disconnect();
        return Result.success(certs);
    }
}
