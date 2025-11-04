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

import de.fraunhofer.iosb.repository.AasRepositoryManager;
import de.fraunhofer.iosb.repository.impl.faaast.FaaastRepositoryManager;
import org.eclipse.edc.spi.result.Result;

import java.util.HashMap;
import java.util.Optional;

public class AasRepositoryRegistry {

    private static final String NULL_VALUE_MESSAGE = "Registry: AAS Repository Manager for %s cannot be null";
    private static final String NOT_EXISTS_MESSAGE = "AAS Repository Manager for %s does not exist";

    private HashMap<Class<? extends AasRepositoryManager<?>>, AasRepositoryManager<?>> managers;

    /**
     * Get an AAS repository manager for an implementation.
     *
     * @param type The manager class. Example: FAAAST.
     * @return Successful result including the AAS repository manager if it exists for this implementation, else failure.
     */
    public <T extends AasRepositoryManager<?>> Result<T> getFor(Class<T> type) {
        var m = managers.get(type);
        return (m != null) ? Result.success(type.cast(m))
                : Result.failure(String.format(NOT_EXISTS_MESSAGE, type.getName()));
    }

    /**
     * Get the default AAS repository manager (FA³ST)
     *
     * @return Successful result including the FA³ST repository manager if it exists, else failure.
     */
    public Result<AasRepositoryManager<?>> getDefault() {
        AasRepositoryManager<?> faaastManager = managers.get(FaaastRepositoryManager.class);
        if (faaastManager != null) {
            return Result.success(faaastManager);
        }
        return Result.failure(String.format(NOT_EXISTS_MESSAGE, FaaastRepositoryManager.class.getSimpleName()));
    }

    /**
     * Register an AAS repository manager to the extension.
     *
     * @param type    The manager class. Example: FAAAST.
     * @param manager The AAS repository manager.
     */
    public <T extends AasRepositoryManager<?>> void register(Class<T> type, AasRepositoryManager<?> manager) {
        Optional.ofNullable(manager)
                .ifPresentOrElse(m -> managers.put(type, m), () -> {
                    throw new IllegalArgumentException(String.format(NULL_VALUE_MESSAGE, type.getSimpleName()));
                });
    }
}
