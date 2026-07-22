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
package de.fraunhofer.iosb.aas.jsonld;

import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static de.fraunhofer.iosb.constants.AasConstants.AAS_PREFIX;
import static de.fraunhofer.iosb.constants.AasConstants.AAS_V30_NAMESPACE;


/**
 * Provides JSON-LD namespace for AAS.
 */
public class JsonLdExtension implements ServiceExtension {

    @Inject
    private JsonLd jsonLdService;

    @Override
    public void initialize(ServiceExtensionContext context) {
        jsonLdService.registerNamespace(AAS_PREFIX, AAS_V30_NAMESPACE);

    }
}
