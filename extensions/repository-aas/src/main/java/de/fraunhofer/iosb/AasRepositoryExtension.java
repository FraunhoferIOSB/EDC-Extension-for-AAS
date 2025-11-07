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

import de.fraunhofer.iosb.repository.impl.faaast.FaaastRepositoryManager;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static de.fraunhofer.iosb.repository.impl.faaast.FaaastRepositoryManager.FAAAST;

/**
 * Instantiate and manage AAS repository implementations.
 */
@Provides(AasRepositoryRegistry.class)
@Extension(value = AasRepositoryExtension.NAME)
public class AasRepositoryExtension implements ServiceExtension {

    public static final String NAME = "AAS Repository Management Extension";

    @Inject
    private Hostname hostname;

    @Provider
    public AasRepositoryRegistry provideAasRepositoryRegistry(ServiceExtensionContext context) {
        AasRepositoryRegistry registry = new AasRepositoryRegistry();

        registry.register(FaaastRepositoryManager.class, new FaaastRepositoryManager(context.getMonitor().withPrefix(FAAAST), hostname));

        return registry;
    }
}
