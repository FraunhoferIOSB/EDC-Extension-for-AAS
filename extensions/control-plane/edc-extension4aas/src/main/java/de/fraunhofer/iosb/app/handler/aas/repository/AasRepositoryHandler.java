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
import de.fraunhofer.iosb.client.exception.UnauthorizedException;
import de.fraunhofer.iosb.client.repository.AasRepositoryClient;
import de.fraunhofer.iosb.ilt.faaast.service.util.ReferenceHelper;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.util.AasUtils;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Identifiable;
import org.eclipse.digitaltwin.aas4j.v3.model.Referable;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementCollection;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementList;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultExtension;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public abstract class AasRepositoryHandler<C extends AasRepositoryClient> extends AasHandler<C> {

    protected AasRepositoryHandler(Monitor monitor, C client, EdcStoreHandler edcStoreHandler) {
        super(monitor, client, edcStoreHandler);
    }


    protected Environment getEnvironment() throws UnauthorizedException, ConnectException {
        return client.getEnvironment();
    }


    @Override
    protected PolicyBinding policyBindingFor(Reference reference) {
        return client.getPolicyBinding(reference);
    }


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


    protected SubmodelElement filterSubmodelElementStructure(Reference parent, SubmodelElement submodelElement) {

        Reference submodelElementReference = AasUtils.toReference(parent, submodelElement);

        if (submodelElement instanceof SubmodelElementList list) {
            List<SubmodelElement> listChildren = list.getValue();
            List<SubmodelElement> filteredChildren = new ArrayList<>();
            // AASd-120 - aware
            for (int i = 0; i < listChildren.size(); i++) {
                SubmodelElement child = listChildren.get(i);
                child.setIdShort(String.valueOf(i));
                var filteredChild = filterSubmodelElementStructure(submodelElementReference, child);

                if (Objects.nonNull(filteredChild)) {
                    filteredChild.setIdShort(null);
                    filteredChildren.add(filteredChild);
                }

            }
            list.setValue(filteredChildren);
        }
        else if (submodelElement instanceof SubmodelElementCollection collection) {
            collection.setValue(collection.getValue().stream()
                    .map(child -> filterSubmodelElementStructure(submodelElementReference, child))
                    .filter(Objects::nonNull)
                    .toList());
        }

        if (client.doRegister(AasUtils.toReference(parent, submodelElement)) ||
                submodelElement instanceof SubmodelElementList list && !list.getValue().isEmpty() ||
                submodelElement instanceof SubmodelElementCollection collection && !collection.getValue().isEmpty()) {
            return submodelElement;
        }
        return null;
    }


    protected SubmodelElement mapSubmodelElement(Reference parent, SubmodelElement submodelElement) {
        Reference submodelElementReference = AasUtils.toReference(parent, submodelElement);
        if (submodelElement instanceof SubmodelElementList list) {
            List<SubmodelElement> value = list.getValue();
            for (int i = 0; i < value.size(); i++) {
                SubmodelElement element = value.get(i);
                element.setIdShort(String.valueOf(i));
                mapSubmodelElement(submodelElementReference, element);
                // AASd-120
                element.setIdShort(null);
            }
        }
        else if (submodelElement instanceof SubmodelElementCollection collection) {
            collection.getValue().forEach(element -> mapSubmodelElement(submodelElementReference, element));
        }

        // We don't want AAS elements that are not registered to be annotated with IDs
        if (client.doRegister(submodelElementReference)) {
            submodelElement.getExtensions().add(new DefaultExtension.Builder()
                    .name(Asset.PROPERTY_ID)
                    .value(submodelElementMapper.generateId(submodelElementReference))
                    .build());
        }
        return submodelElement;
    }

}
