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
package de.fraunhofer.iosb.aas.impl;

import de.fraunhofer.iosb.aas.AasDataProcessorFactory;
import de.fraunhofer.iosb.ssl.SelfSignedCertificateRetriever;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.security.cert.Certificate;
import java.util.Set;

public class RegisteredAasDataProcessorFactory extends AasDataProcessorFactory {

    private final Set<String> registeredAasServices;

    public RegisteredAasDataProcessorFactory(SelfSignedCertificateRetriever retriever, @Nullable Set<String> registeredAasServices) {
        super(retriever);
        this.registeredAasServices = registeredAasServices;
    }

    @Override
    protected Result<Certificate[]> getCertificates(URL url) {
        if (registeredAasServices == null ||
                registeredAasServices.stream().noneMatch(service -> url.toString().startsWith(service))) {
            return Result.failure("AAS service is not registered and allowing all self-signed certificates is disabled");
        }

        return super.retrieveCertificates(url);
    }
}
