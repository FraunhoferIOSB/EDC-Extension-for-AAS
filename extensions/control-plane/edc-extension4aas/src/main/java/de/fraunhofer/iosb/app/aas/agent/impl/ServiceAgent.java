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
package de.fraunhofer.iosb.app.aas.agent.impl;

import de.fraunhofer.iosb.aas.lib.model.impl.Service;
import de.fraunhofer.iosb.app.aas.agent.AasAgent;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.app.pipeline.PipelineFailure;
import de.fraunhofer.iosb.app.pipeline.PipelineResult;
import de.fraunhofer.iosb.client.AasRepositoryClient;
import de.fraunhofer.iosb.client.exception.UnauthorizedException;
import de.fraunhofer.iosb.client.remote.impl.RemoteAasRepositoryClient;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;

import java.net.ConnectException;
import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Communicating with AAS service
 */
public class ServiceAgent extends AasAgent<Service, Environment> {

    public ServiceAgent(EdcHttpClient edcHttpClient, Monitor monitor) {
        super(edcHttpClient, monitor);
    }

    /**
     * Returns the environment of an AAS service.
     *
     * @param service AAS service provider details
     * @return The environment of the service
     */
    @Override
    public PipelineResult<Environment> apply(Service service) {
        try {
            return readEnvironment(service);
        } catch (Exception e) {
            // uncaught exception!
            return PipelineResult.failure(PipelineFailure.warning(List.of(e.getClass().getSimpleName(),
                    e.getMessage())));
        }
    }

    private PipelineResult<Environment> readEnvironment(Service service) {
        HttpClient.Builder httpClientBuilder = service.getAuth().httpClientBuilderFor();

        if (Configuration.getInstance().isAllowSelfSignedCertificates()) {
            try {
                httpClientBuilder.sslContext(getSelfSignedAcceptingContext());
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                return PipelineResult.failure(PipelineFailure.fatal(List.of("Cannot build HttpClient for this AAS repository.")));
            }
        }

        AasRepositoryClient client = new RemoteAasRepositoryClient(service.baseUri(), httpClientBuilder.build());

        if (!client.isAvailable()) {
            return PipelineResult.failure(PipelineFailure.warning(List.of("AAS Repository not available")));
        }

        try {
            return PipelineResult.success(client.getEnvironment());
        } catch (ConnectException e) {
            return PipelineResult.failure(PipelineFailure.warning(List.of(e.getMessage())));
        } catch (UnauthorizedException e) {
            return PipelineResult.failure(PipelineFailure.fatal(List.of(e.getMessage())));
        }
    }


    public SSLContext getSelfSignedAcceptingContext() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{ new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }

            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        } }, new SecureRandom());

        return sslContext;
    }
}
