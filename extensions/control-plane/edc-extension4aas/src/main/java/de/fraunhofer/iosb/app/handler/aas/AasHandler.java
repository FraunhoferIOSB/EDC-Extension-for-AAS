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
package de.fraunhofer.iosb.app.handler.aas;

import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.app.aas.mapper.referable.SubmodelElementMapper;
import de.fraunhofer.iosb.app.aas.mapper.referable.identifiable.IdentifiableMapper;
import de.fraunhofer.iosb.app.handler.aas.util.EnvironmentVisitor;
import de.fraunhofer.iosb.app.handler.edc.EdcStoreHandler;
import de.fraunhofer.iosb.app.handler.util.MappingHelper;
import de.fraunhofer.iosb.client.AasServerClient;
import de.fraunhofer.iosb.client.exception.UnauthorizedException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.util.AasUtils;
import org.eclipse.digitaltwin.aas4j.v3.model.Blob;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Identifiable;
import org.eclipse.digitaltwin.aas4j.v3.model.MultiLanguageProperty;
import org.eclipse.digitaltwin.aas4j.v3.model.Property;
import org.eclipse.digitaltwin.aas4j.v3.model.Range;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.ReferenceElement;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementCollection;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementList;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultExtension;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.StoreFailure;
import org.eclipse.edc.spi.result.StoreResult;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public abstract class AasHandler<C extends AasServerClient> {

    protected final IdentifiableMapper identifiableMapper;
    protected final SubmodelElementMapper submodelElementMapper;
    protected final Monitor monitor;
    protected final C client;
    private final EdcStoreHandler edcStoreHandler;


    protected AasHandler(Monitor monitor, C client, EdcStoreHandler edcStoreHandler) {
        this.identifiableMapper = new IdentifiableMapper(client);
        this.submodelElementMapper = new SubmodelElementMapper(client);
        this.monitor = monitor;
        this.edcStoreHandler = edcStoreHandler;
        this.client = client;
    }


    /**
     * Returns the environment of the AAS server. This could be the environment of an AAS repository or the shell/submodel descriptors of an AAS registry converted into an
     * environment.
     *
     * @return The environment representing this AAS server
     * @throws UnauthorizedException A call to the AAS was returned with a Status code of 401 or 403.
     * @throws ConnectException A connection to the underlying AAS was unsuccessful.
     */
    protected abstract Environment getEnvironment() throws UnauthorizedException, ConnectException;


    /* Override this if your implementation stores which assets are currently registered. */
    protected Map<PolicyBinding, Asset> getCurrentlyRegistered() {
        Environment currentEnvironment;
        try {
            currentEnvironment = getEnvironment();
        }
        catch (UnauthorizedException | ConnectException e) {
            throw new RuntimeException(e);
        }

        Map<Reference, Asset> mapped = MappingHelper.map(currentEnvironment, identifiableMapper::map, submodelElementMapper::map);

        return mapped.entrySet().stream()
                .filter(entry -> client.doRegister(entry.getKey()))
                .map(entry -> Map.entry(policyBindingFor(entry.getKey()), entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }


    /* Initial population of EDC stores. */
    protected Map<PolicyBinding, Asset> initialize() throws UnauthorizedException, ConnectException {
        Environment currentEnvironment = getEnvironment();

        Map<Reference, Asset> mapped = MappingHelper.map(currentEnvironment, identifiableMapper::map, submodelElementMapper::map);

        Stream<Map.Entry<PolicyBinding, Asset>> filtered = mapped.entrySet().stream()
                .filter(entry -> client.doRegister(entry.getKey()))
                .map(entry -> Map.entry(policyBindingFor(entry.getKey()), entry.getValue()));

        var registered = filtered
                .filter(entry -> registerSingle(entry.getKey(), entry.getValue()).succeeded())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        monitor.info(String.format("Registered %s AAS elements from repository %s.", registered.size(), client.getUri()));

        return registered;
    }


    public void cleanUp() {
        monitor.info("Unregistering...");
        Map<PolicyBinding, Asset> filtered = getCurrentlyRegistered();

        List<String> unregisterFailedMessages = filtered.entrySet().stream()
                .map(entry -> unregisterSingle(entry.getKey(), entry.getValue().getId()))
                .filter(AbstractResult::failed)
                .map(StoreResult::getFailureDetail)
                .toList();

        monitor.warning(String.format("Failed unregistering assets with IDs %s", unregisterFailedMessages));

        monitor.info(String.format("Unregistered %s AAS elements from repository %s.", filtered.size() - unregisterFailedMessages.size(),
                client.getUri()));
    }


    protected StoreResult<Void> registerSingle(PolicyBinding policyBinding, Asset asset) {
        StoreResult<Void> storeResult = edcStoreHandler.register(policyBinding, asset);
        if (storeResult.succeeded()) {
            return StoreResult.success();
        }

        // note: if register is called but the AssetIndex already contains it, update it.
        if (storeResult.reason() == StoreFailure.Reason.ALREADY_EXISTS) {
            return updateSingle(policyBinding, asset);
        }
        else {
            monitor.warning(storeResult.getFailureDetail());
            return StoreResult.generalError(storeResult.getFailureDetail());
        }
    }


    protected StoreResult<Void> updateSingle(PolicyBinding policyBinding, Asset asset) {
        StoreResult<Asset> storeResultWithAsset = edcStoreHandler.update(asset);
        if (storeResultWithAsset.succeeded()) {
            return StoreResult.success();
        }

        // note: if an update is called but the AssetIndex cannot find it, create the asset.
        if (storeResultWithAsset.reason() == StoreFailure.Reason.NOT_FOUND) {
            return registerSingle(policyBinding, asset);
        }
        else {
            monitor.warning(storeResultWithAsset.getFailureDetail());
            return StoreResult.generalError(storeResultWithAsset.getFailureDetail());
        }
    }


    protected StoreResult<Void> unregisterSingle(PolicyBinding policyBinding, String assetId) {
        StoreResult<Void> storeResult = edcStoreHandler.unregister(policyBinding, assetId);
        if (storeResult.succeeded() || storeResult.reason() == StoreFailure.Reason.NOT_FOUND) {
            return StoreResult.success();
        }
        else {
            monitor.warning(storeResult.getFailureDetail());
            return StoreResult.generalError(storeResult.getFailureDetail());
        }
    }


    /**
     * Returns the self-description entity for this AAS. The self-description is essentially a representation of this AAS preserving its structure, extended by EDC information for
     * data space consumers to get the necessary information to negotiate the data represented by an AAS element.
     *
     * @return The self-description (An AAS Environment with EDC information added as AAS.Extensions using the HasExtension property of an AAS referable).
     * @throws UnauthorizedException A call to the AAS was returned with a Status code of 401 or 403.
     * @throws ConnectException A connection to the underlying AAS was unsuccessful.
     */
    public final Environment buildSelfDescription() throws UnauthorizedException, ConnectException {
        Function<Identifiable, Identifiable> identifiableMapper = getSelfDescriptionIdentifiableMapper();
        Predicate<Identifiable> identifiableFilter = identifiable -> {
            if (client.doRegister(AasUtils.toReference(identifiable))) {
                return true;
            }
            return (identifiable instanceof Submodel submodel) && !submodel.getSubmodelElements().isEmpty();
        };

        return new EnvironmentVisitor(getEnvironment())
                .visitShells(identifiableFilter)
                .visitShells(identifiableMapper)
                .visitConceptDescriptions(identifiableFilter)
                .visitConceptDescriptions(identifiableMapper)
                .visitSubmodels(identifiableFilter, this::filterSubmodelElementStructure)
                .visitSubmodels(identifiableMapper, this::mapSubmodelElement)
                .environment();
    }


    protected Function<Identifiable, Identifiable> getSelfDescriptionIdentifiableMapper() {
        return identifiable -> {
            if (!(identifiable instanceof Submodel) || client.doRegister(AasUtils.toReference(identifiable))) {
                identifiable.getExtensions().add(new DefaultExtension.Builder()
                        .name(Asset.PROPERTY_ID)
                        .value(this.identifiableMapper.generateId(AasUtils.toReference(identifiable)))
                        .build());
            }
            return identifiable;
        };
    }


    protected PolicyBinding policyBindingFor(Reference identifiableReference) {
        return PolicyBinding.ofDefaults(identifiableReference);
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
        } // TODO make this configurable
        else if (submodelElement instanceof Property property) {
            property.setValue(null);
        }
        else if (submodelElement instanceof Blob blob) {
            blob.setValue(null);
            blob.setContentType(null);
        }
        else if (submodelElement instanceof MultiLanguageProperty multiLanguageProperty) {
            multiLanguageProperty.setValue(null);
            multiLanguageProperty.setValueId(null);
        }
        else if (submodelElement instanceof Range range) {
            range.setMin(null);
            range.setMax(null);
            range.setValueType(null);
        }
        else if (submodelElement instanceof ReferenceElement referenceElement) {
            referenceElement.setValue(null);
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
}
