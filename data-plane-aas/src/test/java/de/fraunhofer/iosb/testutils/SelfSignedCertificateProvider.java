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
import de.fraunhofer.iosb.ilt.faaast.service.config.CoreConfig;
import de.fraunhofer.iosb.ilt.faaast.service.config.ServiceConfig;
import de.fraunhofer.iosb.ilt.faaast.service.endpoint.http.HttpEndpointConfig;
import de.fraunhofer.iosb.ilt.faaast.service.exception.ConfigurationException;
import de.fraunhofer.iosb.ilt.faaast.service.messagebus.internal.MessageBusInternalConfig;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.memory.PersistenceInMemoryConfig;
import de.fraunhofer.iosb.ilt.faaast.service.starter.util.ServiceConfigHelper;
import de.fraunhofer.iosb.ssl.impl.DefaultSelfSignedCertificateRetriever;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.security.cert.Certificate;

public class SelfSignedCertificateProvider {

    private SelfSignedCertificateProvider() {
    }

    public static Certificate[] getSelfSignedCertificate() throws IOException {
        var port = 49687;
        Service service = getService(49687);
        try {
            service.start();
        } catch (Exception faaastServiceException) {
            throw new RuntimeException(faaastServiceException);
        }

        var url = new URL("https://localhost:" + port);
        var certResult = new DefaultSelfSignedCertificateRetriever().getSelfSignedCertificate(url);

        service.stop();

        return certResult.getContent();
    }

    public static Service getService(int port) {
        var serviceConfig = new ServiceConfig.Builder()
                .core(new CoreConfig.Builder().requestHandlerThreadPoolSize(2).build())
                .endpoint(new HttpEndpointConfig.Builder().port(port).build())
                .persistence(PersistenceInMemoryConfig.builder()
                        .initialModelFile(Path.of("src/test/resources/aasEnvironment.json")
                                .toFile())
                        .build())
                .messageBus(new MessageBusInternalConfig())
                .build();
        ServiceConfigHelper.autoComplete(serviceConfig);

        try {
            return new Service(serviceConfig);
        } catch (ConfigurationException | AssetConnectionException e) {
            throw new RuntimeException(e);
        }
    }

}
