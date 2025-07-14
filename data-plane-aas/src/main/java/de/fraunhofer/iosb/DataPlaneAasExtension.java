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
package de.fraunhofer.iosb;

import de.fraunhofer.iosb.aas.lib.AasDataProcessorFactory;
import de.fraunhofer.iosb.aas.lib.impl.AllAasDataProcessorFactory;
import de.fraunhofer.iosb.dataplane.aas.pipeline.AasDataSinkFactory;
import de.fraunhofer.iosb.dataplane.aas.pipeline.AasDataSourceFactory;
import de.fraunhofer.iosb.ssl.SelfSignedCertificateRetriever;
import de.fraunhofer.iosb.ssl.impl.DefaultSelfSignedCertificateRetriever;
import de.fraunhofer.iosb.ssl.impl.NoOpSelfSignedCertificateRetriever;
import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

/**
 * Provides support for communicating with AAS services.
 * Specifically this is:
 * <ul>
 *     <li>
 *         A data address where instead of a URL path,
 *         a reference chain to an AAS element is stored:
 *         (Submodel X, SubmodelElementCollection Y, SubmodelElement Z).
 *     </li>
 *     <li>
 *         An interface allowing the registration of AAS services with
 *         self-signed SSL certificates this EDC is allowed to communicate with.
 *     </li>
 * </ul>
 */
@Provides({AasDataProcessorFactory.class})
@Extension(value = DataPlaneAasExtension.NAME)
public class DataPlaneAasExtension implements ServiceExtension {

    public static final String NAME = "Data Plane AAS";

    private static final String FOREIGN_SELF_SIGNED = "edc.dataplane.aas.acceptForeignSelfSignedCertificates";
    private static final String OWN_SELF_SIGNED = "edc.dataplane.aas.acceptOwnSelfSignedCertificates";

    @Inject
    private PipelineService pipelineService;
    @Inject
    private OkHttpClient okHttpClient;
    @Inject
    private RetryPolicy<Response> retryPolicy;

    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var allowForeignSelfSigned = context.getSetting(FOREIGN_SELF_SIGNED, false);
        var allowOwnSelfSigned = context.getSetting(OWN_SELF_SIGNED, false);

        var aasDataSourceFactory = getAasDataSourceFactory(allowOwnSelfSigned, monitor);
        pipelineService.registerFactory(aasDataSourceFactory);
        var aasDataSinkFactory = getAasDataSinkFactory(allowForeignSelfSigned, monitor);
        pipelineService.registerFactory(aasDataSinkFactory);
    }

    private AasDataSourceFactory getAasDataSourceFactory(boolean ownSelfSigned, Monitor monitor) {
        SelfSignedCertificateRetriever certRetriever;
        if (ownSelfSigned) {
            certRetriever = new DefaultSelfSignedCertificateRetriever();
        } else {
            certRetriever = new NoOpSelfSignedCertificateRetriever();
        }

        var aasDataProcessorFactory = new AllAasDataProcessorFactory(certRetriever, okHttpClient, retryPolicy, monitor);
        return new AasDataSourceFactory(monitor, aasDataProcessorFactory);
    }

    private AasDataSinkFactory getAasDataSinkFactory(boolean foreignSelfSigned, Monitor monitor) {
        SelfSignedCertificateRetriever certRetriever;
        if (foreignSelfSigned) {
            certRetriever = new DefaultSelfSignedCertificateRetriever();
        } else {
            certRetriever = new NoOpSelfSignedCertificateRetriever();
        }

        var aasDataProcessorFactory = new AllAasDataProcessorFactory(certRetriever, okHttpClient, retryPolicy, monitor);
        return new AasDataSinkFactory(monitor, aasDataProcessorFactory);
    }
}
