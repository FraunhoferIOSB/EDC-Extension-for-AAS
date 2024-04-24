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
package de.fraunhofer.iosb.app.util;


import de.fraunhofer.iosb.app.Logger;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.spi.EdcException;

import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.Objects;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import static java.lang.String.format;

public class HttpRestClient {

    private final Logger logger;
    private OkHttpClient client;

    public HttpRestClient() {
        logger = Logger.getInstance();
        this.client = new OkHttpClient();
    }

    /**
     * Issue a get request to a given url
     *
     * @param url the url to where the get request goes
     * @return Response by the service behind the url
     */
    public Response get(URL url) throws IOException {
        logger.debug("GET " + url);
        var request = new Request.Builder()
                .url(Objects.requireNonNull(HttpUrl.get(url)))
                .get()
                .build();
        return client.newCall(request).execute();
    }

    /**
     * Issue a put request to a given url
     *
     * @param url     the url to where the put request goes
     * @param payload payload of this operation
     * @return Response by the service behind the url
     */
    public Response put(URL url, String payload) throws IOException {
        logger.debug("PUT " + url);
        var request = new Request.Builder()
                .url(Objects.requireNonNull(HttpUrl.get(url)))
                .put(RequestBody.create(payload, MediaType.parse("application/json")))
                .build();
        return client.newCall(request).execute();
    }

    /**
     * Issue a post request to a given url
     *
     * @param url     the url to where the post request goes
     * @param payload payload of this operation
     * @return Response by the service behind the url
     */
    public Response post(URL url, String payload) throws IOException {
        logger.debug("POST " + url);
        var request = new Request.Builder()
                .url(Objects.requireNonNull(HttpUrl.get(url)))
                .post(RequestBody.create(payload, MediaType.parse("application/json")))
                .build();
        return client.newCall(request).execute();
    }

    /**
     * Issue a delete request to a given url
     *
     * @param url     the url to where the post request goes
     * @param payload payload of this operation
     * @return Response by the service behind the url
     */
    public Response delete(URL url, String payload) throws IOException {
        logger.debug("DELETE " + url);

        RequestBody requestBody = null;
        if (Objects.nonNull(payload)) {
            requestBody = RequestBody.create(payload, MediaType.parse("application/json"));
        }
        var request = new Request.Builder()
                .url(Objects.requireNonNull(HttpUrl.get(url)))
                .delete(requestBody)
                .build();
        return client.newCall(request).execute();
    }

    /**
     * Add self-signed certificates to trust when executing http requests.
     *
     * @param certs All self-signed certificates to trust.
     * @throws KeyStoreException        generic keyStore exception to throw at user (maybe some OS error)
     * @throws NoSuchAlgorithmException When a particular cryptographic algorithm is requested but is not available in the environment.
     */
    public void setAcceptedSelfSignedCertificates(Map<String, Certificate[]> certs) throws KeyStoreException, NoSuchAlgorithmException {
        var keyStore = createAndPopulateKeyStore(certs);

        var tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);

        var trustManagers = tmf.getTrustManagers();

        SSLContext sslContext = SSLContext.getInstance("TLS");

        // No KeyManager needed: we don't need to authenticate ourselves, only trust "others"
        try {
            sslContext.init(null, trustManagers, null);
        } catch (KeyManagementException keyManagementException) {
            throw new EdcException(
                    format("Could not set self-signed certificate for certificates from %s", certs.keySet()),
                    keyManagementException);
        }

        client = new OkHttpClient()
                .newBuilder()
                .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustManagers[0])
                .build();
    }

    private KeyStore createAndPopulateKeyStore(Map<String, Certificate[]> certs) throws KeyStoreException {
        // Create an empty KeyStore
        var keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

        try {
            keyStore.load(null, null);
        } catch (CertificateException | IOException | NoSuchAlgorithmException keyStoreLoadException) {
            // Can not be thrown since we input null
            throw new EdcException(
                    format("Could not set self-signed certificate for certificates from %s", certs.keySet()),
                    keyStoreLoadException);
        }

        // Add each certificate for each service to the KeyStore
        for (var entry : certs.entrySet()) {
            var serviceUrl = entry.getKey();
            var certificates = entry.getValue();

            for (var cert : certificates) {
                keyStore.setCertificateEntry(serviceUrl + cert.hashCode(), cert);
            }
        }
        return keyStore;
    }
}
