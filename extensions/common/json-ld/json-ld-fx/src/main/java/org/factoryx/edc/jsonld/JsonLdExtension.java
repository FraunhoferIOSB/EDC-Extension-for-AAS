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
package org.factoryx.edc.jsonld;

import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.io.File;
import java.util.Map;

import static org.eclipse.tractusx.edc.core.utils.FileUtils.getResourceFile;


/**
 * Provides JSON-LD structure for Factory-X policies.
 */
public class JsonLdExtension implements ServiceExtension {
    public static final String FX_POLICY_NS = "https://w3id.org/factoryx/policy/v1.0/";
    public static final String FX_POLICY_PREFIX = "fx-policy";
    public static final String FX_POLICY_CONTEXT = FX_POLICY_NS + "context.jsonld";

    public static final String FX_CONTEXT = "https://w3id.org/factoryx/edc/v0.0.1";

    public static final String FX_CREDENTIAL_NS = "https://w3id.org/factoryx/credentials/v1.0/";
    public static final String FX_CREDENTIAL_PREFIX = "fx-credentials";
    public static final String FX_CREDENTIAL_CONTEXT = FX_CREDENTIAL_NS + "context.jsonld";

    private static final String PREFIX = "document" + File.separator;
    private static final Map<String, String> FILES = Map.of(
            FX_POLICY_CONTEXT, PREFIX + "fx-policy-v1.jsonld",
            FX_CREDENTIAL_CONTEXT, PREFIX + "fx-credentials-v1.jsonld");

    @Inject
    private JsonLd jsonLdService;

    @Inject
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        jsonLdService.registerNamespace(FX_POLICY_PREFIX, FX_POLICY_NS);

        var files = FILES.entrySet().stream().map(this::mapToFile).toList();

        for (var file: files) {
            if (file.succeeded()) {
                var x = file.getContent().getKey();
                var y = file.getContent().getValue().toURI();
                jsonLdService.registerCachedDocument(file.getContent().getKey(), file.getContent().getValue().toURI());
            }
        }

        FILES.entrySet().stream().map(this::mapToFile)
                .forEach(result -> result.onSuccess(entry -> jsonLdService.registerCachedDocument(entry.getKey(), entry.getValue().toURI()))
                        .onFailure(failure -> monitor.warning("Failed to register cached json-ld document: " + failure.getFailureDetail())));
    }


    private Result<Map.Entry<String, File>> mapToFile(Map.Entry<String, String> fileEntry) {
        return getResourceFile(fileEntry.getValue())
                .map(file1 -> Map.entry(fileEntry.getKey(), file1));
    }

}
