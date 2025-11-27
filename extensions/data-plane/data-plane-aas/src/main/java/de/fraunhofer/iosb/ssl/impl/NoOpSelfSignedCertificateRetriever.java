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

import java.security.cert.Certificate;


/**
 * Does not return self-signed certificates. This implementation is used when the extension configuration does not allow any self-signed certificates.
 */
public class NoOpSelfSignedCertificateRetriever implements SelfSignedCertificateRetriever {
    @Override
    public Result<Certificate[]> getSelfSignedCertificate(String urlString) {
        return Result.failure("Self-signed certificates not allowed");
    }
}
