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
package de.fraunhofer.iosb.client.local.impl;

import de.fraunhofer.iosb.client.exception.NotFoundException;
import de.fraunhofer.iosb.client.local.LocalAasRepositoryClient;
import de.fraunhofer.iosb.model.context.impl.FaaastRepositoryContext;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Referable;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEnvironment;

public class LocalFaaastRepositoryClient extends LocalAasRepositoryClient<FaaastRepositoryContext> {

    public LocalFaaastRepositoryClient(FaaastRepositoryContext context) {
        super(context);
    }


    @Override
    public Environment getEnvironment() {
        return new DefaultEnvironment.Builder()
                .assetAdministrationShells(context.getAllAas())
                .submodels(context.getAllSubmodels())
                .conceptDescriptions(context.getAllConceptDescriptions())
                .build();
    }


    @Override
    public <R extends Referable> R getReferable(Reference reference, Class<R> clazz) throws NotFoundException {
        return context.getReferable(reference, clazz);
    }
}
