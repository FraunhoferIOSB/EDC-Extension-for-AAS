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
package de.fraunhofer.iosb.aas.lib;

import de.fraunhofer.iosb.ssl.SelfSignedCertificateRetriever;
import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.eclipse.edc.http.client.EdcHttpClientImpl;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.security.cert.Certificate;

import static de.fraunhofer.iosb.aas.lib.http.HttpClientProvider.clientFor;
import static de.fraunhofer.iosb.ssl.impl.DefaultSelfSignedCertificateRetriever.isTrusted;

public abstract class AasDataProcessorFactory {

    private static final String HTTPS = "HTTPS";
    protected final SelfSignedCertificateRetriever retriever;
    // EDC provided fields. Used for non-self-signed certificates
    private final OkHttpClient edcOkHttpClient;
    private final RetryPolicy<Response> edcRetryPolicy;
    private final Monitor monitor;

    public AasDataProcessorFactory(SelfSignedCertificateRetriever retriever,
                                   OkHttpClient edcOkHttpClient,
                                   RetryPolicy<Response> edcRetryPolicy,
                                   Monitor monitor) {
        this.retriever = retriever;

        this.monitor = monitor;
        this.edcOkHttpClient = edcOkHttpClient;
        this.edcRetryPolicy = edcRetryPolicy;
    }

    /**
     * Return a processor accepting the certificates of the server behind the given URL.
     * This is for AAS services with self-signed certificates.
     * Allowing self-signed certificates can be configured (see readme).
     *
     * @param aasUrl URL of AAS service without element access suffix (e.g., /submodels)
     * @return AAS Processor allowing communication with AAS service using AAS data addresses
     */
    public Result<AasDataProcessor> processorFor(URL aasUrl) {
        if (!aasUrl.getProtocol().equalsIgnoreCase(HTTPS)) {
            return Result.success(new AasDataProcessor(
                    new EdcHttpClientImpl(edcOkHttpClient, edcRetryPolicy, monitor)));
        }

        var certResult = this.retriever.getSelfSignedCertificate(aasUrl);

        var client = edcOkHttpClient;

        if (certResult.succeeded() && certResult.getContent() != null) {
            var customClientResult = clientFor(certResult.getContent());

            if (customClientResult.succeeded()) {
                client = customClientResult.getContent();
            } else {
                return Result.failure(customClientResult.getFailureDetail());
            }
        } else if (certResult.succeeded() && certResult.getContent() == null) {
            monitor.debug("%s is trusted".formatted(aasUrl));
        } else {
            monitor.info("Did not retrieve certificates for %s: %s".formatted(aasUrl, certResult.getFailureDetail()));
        }

        return Result.success(new AasDataProcessor(new EdcHttpClientImpl(client, edcRetryPolicy, monitor)));
    }

    protected abstract Result<Certificate[]> getCertificates(URL url);

    protected Result<@Nullable Certificate[]> retrieveCertificates(URL aasUrl) {
        if (isTrusted(aasUrl) || "http".equalsIgnoreCase(aasUrl.getProtocol())) {
            return Result.success(null);
        }

        var certsResult = retriever.getSelfSignedCertificate(aasUrl);

        if (certsResult.failed()) {
            return Result.failure("Certificates were neither trusted nor self-signed: %s"
                    .formatted(certsResult.getFailureMessages()));
        }

        return certsResult;
    }
}
