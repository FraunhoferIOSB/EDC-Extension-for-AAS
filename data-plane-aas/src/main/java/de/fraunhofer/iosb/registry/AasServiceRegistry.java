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
package de.fraunhofer.iosb.registry;

import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.Set;

/**
 * If config value edc.dataplane.aas.acceptOwnSelfSignedCertificates is True,
 * register AAS services with potentially self-signed certificates. Else, registering
 * AAS services here will have no effect.
 */
public class AasServiceRegistry {

    private final Set<String> registeredAasServices;

    public AasServiceRegistry(@Nullable Set<String> registeredAasServices) {
        this.registeredAasServices = registeredAasServices;
    }

    public Result<Void> register(URL aasServiceUrl) {
        if (registeredAasServices == null) {
            return Result.failure("edc.dataplane.aas.acceptOwnSelfSignedCertificates is set to False");
        }
        registeredAasServices.add(aasServiceUrl.toString());
        return Result.success();
    }

    public void unregister(URL aasServiceUrl) {
        if (registeredAasServices != null) {
            registeredAasServices.remove(aasServiceUrl.toString());
        }
    }
}
