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
package de.fraunhofer.iosb.app.pipeline;

import de.fraunhofer.iosb.app.pipeline.helper.Filter;
import de.fraunhofer.iosb.client.local.impl.LocalFaaastRepositoryClient;
import de.fraunhofer.iosb.ilt.faaast.service.util.ReferenceHelper;
import de.fraunhofer.iosb.model.context.impl.FaaastRepositoryContext;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.internal.serialization.EnumSerializer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.internal.util.ReflectionHelper;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.util.AasUtils;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.Referable;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.List;
import java.util.Objects;

public class LocalFaaastRepositoryPipeline extends Pipeline<Void, Void> {

    private final FaaastRepositoryContext context;
    private final LocalFaaastRepositoryClient client;


    public LocalFaaastRepositoryPipeline(Monitor monitor, FaaastRepositoryContext faaastContext) {
        super(List.of(), monitor);
        this.context = faaastContext;
        this.client = new LocalFaaastRepositoryClient(context);
        pipelineSteps.add(new Filter<>((o) -> isAvailable()));
    }

    private boolean isAvailable() {
        return context.isAvailable();
    }

    private Environment getEnvironment() {
        return client.getEnvironment();
    }

    private <R extends Referable> List<? extends R> filter(Environment environment) {
        return context.getReferences().stream()
                .map(reference -> {
                    KeyTypes key = ReferenceHelper.getEffectiveKeyType(reference);

                    Class<R> clazz = ReflectionHelper.INTERFACES.stream()
                            .filter(cls -> cls.getSimpleName().equals(EnumSerializer.serializeEnumName(key.name())))
                            .map(cls -> (Class<R>) cls) // Safe as long as the class comes from ReflectionHelper.INTERFACES
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("No class match for key: " + key));

                    return AasUtils.resolve(reference, environment, clazz);
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
