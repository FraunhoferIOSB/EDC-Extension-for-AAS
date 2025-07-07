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
package de.fraunhofer.iosb.testutils;

import de.fraunhofer.iosb.ilt.faaast.service.Service;
import de.fraunhofer.iosb.ilt.faaast.service.assetconnection.AssetConnectionException;
import de.fraunhofer.iosb.ilt.faaast.service.config.ServiceConfig;
import de.fraunhofer.iosb.ilt.faaast.service.endpoint.http.HttpEndpointConfig;
import de.fraunhofer.iosb.ilt.faaast.service.exception.ConfigurationException;
import de.fraunhofer.iosb.ilt.faaast.service.exception.EndpointException;
import de.fraunhofer.iosb.ilt.faaast.service.exception.MessageBusException;
import de.fraunhofer.iosb.ilt.faaast.service.model.exception.PersistenceException;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.memory.PersistenceInMemoryConfig;
import de.fraunhofer.iosb.ilt.faaast.service.starter.util.ServiceConfigHelper;
import de.fraunhofer.iosb.ssl.impl.DefaultSelfSignedCertificateRetriever;

import java.net.URL;
import java.nio.file.Path;
import java.security.cert.Certificate;

import static org.eclipse.edc.util.io.Ports.getFreePort;

public class TestUtils {

    public Certificate[] getSelfSignedCertificate() {
        var port = getFreePort();

        try (var ignored = startFaaastService(port)) {
            var url = new URL("https://localhost:" + port);
            var certResult = new DefaultSelfSignedCertificateRetriever().getSelfSignedCertificate(url);
            return certResult.getContent();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns autocloseable handle to FA³ST service
     *
     * @param port port for FA³ST to use
     * @return AutoCloseable handle on FA³ST service
     */
    public ServiceHandle startFaaastService(int port) throws ConfigurationException, AssetConnectionException,
            MessageBusException, EndpointException, PersistenceException {
        var serviceConfig = new ServiceConfig.Builder()
                .endpoint(new HttpEndpointConfig.Builder().port(port).build())
                .persistence(PersistenceInMemoryConfig.builder()
                        .initialModelFile(Path.of("src/test/resources/aasEnvironment.json")
                                .toFile())
                        .build())
                .build();

        ServiceConfigHelper.autoComplete(serviceConfig);

        return new ServiceHandle(new Service(serviceConfig));
    }

    /**
     * AutoCloseable handle on FA³ST service. Service gets started on object creation and stopped on object destruction
     */
    public static class ServiceHandle implements AutoCloseable {

        private final Service service;

        ServiceHandle(Service service) throws MessageBusException, EndpointException, PersistenceException {
            this.service = service;
            this.service.start();
        }

        @Override
        public void close() {
            if (service != null) {
                service.stop();
            }
        }
    }
}
