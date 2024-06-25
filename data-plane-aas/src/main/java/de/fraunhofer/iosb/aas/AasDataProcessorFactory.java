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
package de.fraunhofer.iosb.aas;

import de.fraunhofer.iosb.ssl.SelfSignedCertificateRetriever;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStoreException;
import java.security.cert.Certificate;

import static de.fraunhofer.iosb.aas.http.HttpClientProvider.clientFor;
import static java.lang.String.format;

public abstract class AasDataProcessorFactory {

    private static final String HTTPS = "HTTPS";
    protected final SelfSignedCertificateRetriever retriever;

    public AasDataProcessorFactory(SelfSignedCertificateRetriever retriever) {
        this.retriever = retriever;
    }

    /**
     * Return a processor for a given URL.
     * This is for AAS services with self-signed certificates.
     * Allowing self-signed certificates can be configured (see readme).
     *
     * @param urlString URL of AAS service.
     * @return AAS Processor allowing communication with AAS service using AAS data addresses
     */
    public AasDataProcessor processorFor(String urlString) {
        URL aasUrl;
        try {
            aasUrl = new URL(urlString);
        } catch (MalformedURLException malformedUrlException) {
            throw new EdcException("Malformed URL for AAS manipulator", malformedUrlException);
        }

        var certResult = getCertificates(aasUrl);

        try {
            return new AasDataProcessor(clientFor(certResult.getContent()));
        } catch (KeyStoreException e) {
            throw new EdcException(e);
        }
    }

    protected abstract Result<Certificate[]> getCertificates(URL url);

    protected Result<Certificate[]> retrieveCertificates(URL url) {
        if (!url.getProtocol().equalsIgnoreCase(HTTPS)) {
            // Returning no certificates means accepting only trusted authorities (and http)
            return Result.success(null);
        }

        var certsResult = retriever.getSelfSignedCertificate(url);

        if (certsResult.failed() && !certsResult.getFailureMessages().contains("trusted")) {
            throw new EdcException(format("Certificates were neither trusted nor self-signed: %s",
                    certsResult.getFailureMessages()));
        }

        return certsResult;
    }
}
