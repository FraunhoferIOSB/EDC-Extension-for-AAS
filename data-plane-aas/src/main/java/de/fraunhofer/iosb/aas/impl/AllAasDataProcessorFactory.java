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
import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.security.cert.Certificate;


public class AllAasDataProcessorFactory extends AasDataProcessorFactory {

    public AllAasDataProcessorFactory(SelfSignedCertificateRetriever retriever, OkHttpClient edcOkHttpClient,
                                      RetryPolicy<Response> edcRetryPolicy, Monitor monitor) {
        super(retriever, edcOkHttpClient, edcRetryPolicy, monitor);
    }

    @Override
    protected Result<@Nullable Certificate[]> getCertificates(URL url) {
        return super.retrieveCertificates(url);
    }
}
