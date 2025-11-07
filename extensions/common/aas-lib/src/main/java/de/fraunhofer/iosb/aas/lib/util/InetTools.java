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
package de.fraunhofer.iosb.aas.lib.util;

import de.fraunhofer.iosb.aas.lib.model.AasProvider;
import org.eclipse.edc.spi.result.Result;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Locale;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class InetTools {

    private static final TrustManager[] TRUST_ALL_MANAGER = new TrustManager[]{ new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
    } };

    private InetTools() {
        throw new RuntimeException("Utility class");
    }

    /**
     * <a href="https://stackoverflow.com/questions/3584210/preferred-java-way-to-ping-an-http-url-for-availability">...</a>
     *
     * @param host Host
     * @param port Port
     * @return True if host is reachable under given port within timeout seconds.
     */
    public static boolean pingHost(String host, int port) {
        int actualPort;

        if (port != -1) {
            actualPort = port;
        } else if (host.toLowerCase(Locale.ROOT).startsWith("https")) {
            actualPort = 443;
        } else {
            actualPort = 80;
        }

        try (Socket ignored = new Socket(host, actualPort)) {
            return true;
        } catch (IOException e) {
            return false; // Either timeout or unreachable or failed DNS lookup.
        }
    }

    private static boolean checkUrlAvailability(URI toCheck) {
        try {
            // Open basic http connection with "GET" method and check if IOException occurs
            HttpURLConnection connection = (HttpURLConnection) toCheck.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(7000);
            connection.getResponseCode();
            return true;
        } catch (IOException e) {
            return false;
        }
    }


    public static boolean pingHost(AasProvider provider) {
        var host = provider.baseUri().getHost();
        var port = provider.baseUri().getPort();
        // If no port available, port should be 443 or 80
        if (port == -1) {
            // Iff http:// go with port 80
            port = host.startsWith("http:") ? 80 : 443;
        }

        return pingHost(host, port) || checkUrlAvailability(provider.baseUri());
    }


    public static boolean isConnectionTrusted(URI uri) {
        HttpsURLConnection.setDefaultSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault());

        try {
            var conn = uri.toURL().openConnection();
            if (conn instanceof HttpsURLConnection) {
                conn = uri.toURL().openConnection();
                conn.connect();
                // Connection with standard java library succeeded
                // -> according to this system, the server has a trusted certificate
                return true;
            } else {
                // TODO should we allow unencrypted traffic in production for our services?
                return conn instanceof HttpURLConnection;
            }

        } catch (IOException e) {
            return false;
        }
    }


    public static Result<Certificate[]> getSelfSignedCertificate(URI uri) {
        SSLContext sslContext;

        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, TRUST_ALL_MANAGER, new SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException generalSecurityException) {
            return Result.failure(List.of(generalSecurityException.getMessage()));
        }

        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        HttpsURLConnection conn;

        try {
            conn = (HttpsURLConnection) uri.toURL().openConnection();
            conn.connect();
        } catch (IOException e) {
            return Result.failure(List.of(e.getMessage()));
        }

        X509Certificate[] certs;
        try {
            certs = (X509Certificate[]) conn.getServerCertificates();
        } catch (SSLPeerUnverifiedException e) {
            return Result.failure("peer unverified");
        }

        try {
            for (X509Certificate cert : certs) {
                cert.checkValidity();
            }
        } catch (CertificateExpiredException | CertificateNotYetValidException e) {
            return Result.failure("expired");
        }

        conn.disconnect();
        return Result.success(certs);
    }
}
