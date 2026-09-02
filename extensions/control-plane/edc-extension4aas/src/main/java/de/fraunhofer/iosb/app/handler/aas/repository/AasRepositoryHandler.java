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
package de.fraunhofer.iosb.app.handler.aas.repository;

import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.app.handler.aas.AasHandler;
import de.fraunhofer.iosb.app.handler.edc.EdcStoreHandler;
import de.fraunhofer.iosb.client.repository.AasRepositoryClient;
import de.fraunhofer.iosb.ilt.faaast.client.exception.ConnectivityException;
import de.fraunhofer.iosb.ilt.faaast.client.exception.StatusCodeException;
import de.fraunhofer.iosb.ilt.faaast.service.util.ReferenceHelper;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.util.AasUtils;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Identifiable;
import org.eclipse.digitaltwin.aas4j.v3.model.Referable;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.List;


/**
 * Abstract handler for AAS repositories, providing access to the repository environment and the policy bindings
 * configured per reference.
 *
 * @param <C> AAS repository client implementation to communicate with the AAS repository.
 */
public abstract class AasRepositoryHandler<C extends AasRepositoryClient> extends AasHandler<C> {

    /**
     * Creates a new AAS repository handler.
     *
     * @param monitor Monitor used for log outputs.
     * @param client Client used to communicate with the AAS repository.
     * @param edcStoreHandler Handler to manage registration of EDC assets, policies and contracts.
     */
    protected AasRepositoryHandler(Monitor monitor, C client, EdcStoreHandler edcStoreHandler) {
        super(monitor, client, edcStoreHandler);
    }


    /**
     * Returns the environment of the AAS repository.
     *
     * @return The environment of the AAS repository.
     * @throws StatusCodeException if a call to the AAS repository returned a status code other than 2xx.
     * @throws ConnectivityException if a connection to the AAS repository could not be established.
     */
    protected Environment getEnvironment() throws StatusCodeException, ConnectivityException {
        return client.getEnvironment();
    }


    @Override
    protected List<PolicyBinding> policyBindingsFor(Reference reference) {
        return client.getPolicyBindings(reference);
    }


    /**
     * Maps the referable referenced by the given reference (resolved against the given environment) to an EDC asset.
     * Identifiables are mapped using the identifiable mapper, submodel elements using the submodel element mapper.
     *
     * @param reference Reference of the AAS element to map.
     * @param environment The environment used to resolve the reference.
     * @return The mapped EDC asset.
     */
    protected Asset referenceToAsset(Reference reference, Environment environment) {
        Referable referable = AasUtils.resolve(reference, environment);

        Asset mapped;
        if (referable instanceof Identifiable identifiable) {
            mapped = identifiableMapper.map(identifiable);
        }
        else if (referable instanceof SubmodelElement submodelElement) {
            mapped = submodelElementMapper.map(ReferenceHelper.getParent(reference), submodelElement);
        }
        else {
            throw new EdcException("Could not resolve event message reference.");
        }

        return mapped;
    }

}
