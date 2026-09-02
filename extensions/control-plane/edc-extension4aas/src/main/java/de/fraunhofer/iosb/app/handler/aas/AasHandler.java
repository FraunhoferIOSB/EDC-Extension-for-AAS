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
import de.fraunhofer.iosb.app.aas.mapper.util.AssetIdUtil;
import de.fraunhofer.iosb.app.handler.aas.util.EnvironmentVisitor;
import de.fraunhofer.iosb.app.handler.edc.EdcStoreHandler;
import de.fraunhofer.iosb.app.handler.util.DataAddressMerger;
import de.fraunhofer.iosb.app.handler.util.MappingHelper;
import de.fraunhofer.iosb.client.AasServerClient;
import de.fraunhofer.iosb.ilt.faaast.client.exception.ConnectivityException;
import de.fraunhofer.iosb.ilt.faaast.client.exception.StatusCodeException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.util.AasUtils;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Extension;
import org.eclipse.digitaltwin.aas4j.v3.model.Identifiable;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * Superclass for AAS server handlers to bundle shared functionality.
 *
 * @param <C> AAS server client implementation to communicate with AAS server.
 */
public abstract class AasHandler<C extends AasServerClient> {

    /** Mapper for AAS identifiable (shells, submodels, concept descriptions). */
    protected final IdentifiableMapper identifiableMapper;
    /** Mapper for submodel elements. */
    protected final SubmodelElementMapper submodelElementMapper;
    /** Monitor used for logging. */
    protected final Monitor monitor;
    /** Client used to communicate with the AAS server. */
    protected final C client;
    private final EdcStoreHandler edcStoreHandler;


    /**
     * Creates a new AAS handler.
     *
     * @param monitor Monitor used for logging.
     * @param client Client used to communicate with the AAS server.
     * @param edcStoreHandler Handler for interacting with the EDC stores.
     */
    protected AasHandler(Monitor monitor, C client, EdcStoreHandler edcStoreHandler) {
        this.identifiableMapper = new IdentifiableMapper(client);
        this.submodelElementMapper = new SubmodelElementMapper(client);
        this.monitor = monitor;
        this.edcStoreHandler = edcStoreHandler;
        this.client = client;
    }


    /**
     * Returns the self-description entity for this AAS. The self-description is essentially a representation of this AAS
     * preserving its structure, extended by EDC information for
     * data space consumers to get the necessary information to negotiate the data represented by an AAS element.
     *
     * @return The self-description (An AAS Environment with EDC information added as AAS.Extensions using the HasExtension
     *         property of an AAS referable).
     * @throws StatusCodeException A call to the AAS was returned with a Status code != 2xx.
     * @throws ConnectivityException A connection to the underlying AAS was unsuccessful.
     */
    public final Environment buildSelfDescription() throws StatusCodeException, ConnectivityException {
        Consumer<Identifiable> identifiableVisitor = getSelfDescriptionIdentifiableMapper();
        Predicate<Identifiable> identifiableFilter = identifiable -> {
            if (client.eligibleForRegistration(AasUtils.toReference(identifiable))) {
                return true;
            }
            return (identifiable instanceof Submodel submodel) && !submodel.getSubmodelElements().isEmpty();
        };

        return new EnvironmentVisitor(getEnvironment())
                .visitShells(identifiableFilter)
                .visitShells(identifiableVisitor)
                .visitConceptDescriptions(identifiableFilter)
                .visitConceptDescriptions(identifiableVisitor)
                .visitSubmodels(identifiableFilter, this::filterSubmodelElementStructure)
                .visitSubmodels(identifiableVisitor, this::mapSubmodelElement)
                .environment();
    }


    /**
     * Unregisters all AAS elements currently registered by this handler from the EDC stores.
     */
    public void cleanUp() {
        monitor.info("Unregistering...");
        Map<PolicyBinding, Asset> filtered = getCurrentlyRegistered();

        List<String> unregisterFailedMessages = filtered.entrySet().stream()
                .map(entry -> unregisterSingle(entry.getKey(), entry.getValue().getId()))
                .filter(AbstractResult::failed)
                .map(StoreResult::getFailureDetail)
                .toList();

        if (!unregisterFailedMessages.isEmpty()) {
            monitor.warning(String.format("Failed unregistering assets with IDs %s", unregisterFailedMessages));
        }

        monitor.info(String.format("Unregistered %s AAS elements from repository %s.", filtered.size() - unregisterFailedMessages.size(),
                client.getUri()));
    }


    /**
     * Returns the environment of the AAS server. This could be the environment of an AAS repository or the shell/submodel
     * descriptors of an AAS registry converted into an
     * environment.
     *
     * @return The environment representing this AAS server
     * @throws StatusCodeException A call to the AAS was returned with a Status code != 2xx.
     * @throws ConnectivityException A connection to the underlying AAS was unsuccessful.
     */
    protected abstract Environment getEnvironment() throws ConnectivityException, StatusCodeException;


    /**
     * Returns the assets currently registered by this handler. Override this if your implementation stores which assets
     * are currently registered.
     *
     * @return Mapping of policy bindings to the currently registered assets.
     */
    protected Map<PolicyBinding, Asset> getCurrentlyRegistered() {
        Environment currentEnvironment;
        try {
            currentEnvironment = getEnvironment();
        }
        catch (StatusCodeException | ConnectivityException e) {
            throw new RuntimeException(e);
        }

        Map<Reference, Asset> mapped = MappingHelper.map(currentEnvironment, identifiableMapper::map, submodelElementMapper::map);

        return expandBindings(mapped);
    }


    /**
     * Initial population of EDC stores with the AAS elements of the underlying server.
     *
     * @return Mapping of policy bindings to the assets that were registered.
     * @throws StatusCodeException A call to the AAS was returned with a Status code != 2xx.
     * @throws ConnectivityException A connection to the underlying AAS was unsuccessful.
     */
    protected Map<PolicyBinding, Asset> initialize() throws StatusCodeException, ConnectivityException {
        Environment currentEnvironment = getEnvironment();

        Map<Reference, Asset> mapped = MappingHelper.map(currentEnvironment, identifiableMapper::map, submodelElementMapper::map);

        Map<PolicyBinding, Asset> expanded = expandBindings(mapped);

        var registered = expanded.entrySet().stream()
                .filter(entry -> registerSingle(entry.getKey(), entry.getValue()).succeeded())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        monitor.info(String.format("Registered %s AAS elements from repository %s.", registered.size(), client.getUri()));

        return registered;
    }


    /**
     * Expands the single-asset-per-reference mapping into one asset per applicable policy binding. For each eligible
     * reference, the base asset (as produced by the mappers) is cloned per binding, assigning a binding-specific asset
     * ID and merging the binding's data address properties onto the base data address.
     *
     * @param mapped Base mapping of references to assets (one asset per reference).
     * @return Mapping of policy bindings to binding-specific assets.
     */
    protected Map<PolicyBinding, Asset> expandBindings(Map<Reference, Asset> mapped) {
        Map<PolicyBinding, Asset> result = new HashMap<>();
        mapped.forEach((reference, baseAsset) -> {
            if (client.eligibleForRegistration(reference)) {
                for (PolicyBinding binding: policyBindingsFor(reference)) {
                    result.put(binding, assetForBinding(reference, baseAsset, binding));
                }
            }
        });
        return result;
    }


    /**
     * Builds a binding-specific asset by cloning the base asset and overriding its ID and data address. The binding's
     * {@code dataAddressProperties} are merged onto the base data address, with binding-provided properties taking
     * precedence on key conflict.
     *
     * @param reference Reference of the AAS element.
     * @param baseAsset Base asset produced by the mapper.
     * @param binding Policy binding to apply.
     * @return A new asset specific to the given binding.
     */
    protected Asset assetForBinding(Reference reference, Asset baseAsset, PolicyBinding binding) {
        var mergedDataAddress = DataAddressMerger.merge(baseAsset.getDataAddress(), binding.dataAddressProperties());
        return baseAsset.toBuilder()
                .id(AssetIdUtil.id(client.getUri().toString(), reference, binding))
                .dataAddress(mergedDataAddress)
                .build();
    }


    /**
     * Registers a single asset under the given policy binding in the EDC stores.
     *
     * @param policyBinding Policy binding to register the asset with.
     * @param asset Asset to register.
     * @return Store result containing the state of the operation.
     */
    protected StoreResult<Void> registerSingle(PolicyBinding policyBinding, Asset asset) {
        StoreResult<Void> storeResult = edcStoreHandler.register(policyBinding, asset);
        if (storeResult.succeeded()) {
            return StoreResult.success();
        }

        monitor.warning(storeResult.getFailureDetail());
        return StoreResult.generalError(storeResult.getFailureDetail());
    }


    /**
     * Updates a single asset in the EDC stores.
     *
     * @param asset Asset to update.
     * @return Store result containing the state of the operation.
     */
    protected StoreResult<Void> updateSingle(Asset asset) {
        StoreResult<Asset> storeResultWithAsset = edcStoreHandler.update(asset);
        if (storeResultWithAsset.succeeded()) {
            return StoreResult.success();
        }

        monitor.warning(storeResultWithAsset.getFailureDetail());
        return StoreResult.generalError(storeResultWithAsset.getFailureDetail());
    }


    /**
     * Unregisters a single asset from the EDC stores.
     *
     * @param policyBinding Policy binding the asset was registered with.
     * @param assetId Id of the asset to unregister.
     * @return Store result containing the state of the operation.
     */
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
     * Returns a consumer that annotates identifiables with their EDC asset IDs as AAS extensions.
     *
     * @return Consumer mapping an identifiable to its self-description representation.
     */
    protected Consumer<Identifiable> getSelfDescriptionIdentifiableMapper() {
        return identifiable -> {
            Reference reference = AasUtils.toReference(identifiable);
            if (!(identifiable instanceof Submodel) || client.eligibleForRegistration(reference)) {
                List<Extension> extensions = policyBindingsFor(reference).stream()
                        .map(binding -> buildExtension(AssetIdUtil.id(client.getUri().toString(), reference, binding)))
                        .toList();
                identifiable.setExtensions(extensions);
            }
        };
    }


    /**
     * Returns all policy bindings applicable to the given reference. The default implementation returns a single
     * default binding; subclasses backed by an AAS repository override this to return the configured bindings (one per
     * registered policy/data-address combination).
     *
     * @param identifiableReference Reference of the AAS element.
     * @return List of policy bindings for the reference (never null, possibly empty).
     */
    protected List<PolicyBinding> policyBindingsFor(Reference identifiableReference) {
        return List.of(PolicyBinding.ofDefaults(identifiableReference));
    }


    /**
     * Recursively maps a submodel element, annotating eligible elements with their EDC asset IDs as AAS extensions.
     *
     * @param parent Reference of the parent element.
     * @param submodelElement Submodel element to map.
     * @return The mapped submodel element.
     */
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
        if (client.eligibleForRegistration(submodelElementReference)) {
            List<Extension> extensions = policyBindingsFor(submodelElementReference).stream()
                    .map(binding -> buildExtension(AssetIdUtil.id(client.getUri().toString(), submodelElementReference, binding)))
                    .toList();
            submodelElement.setExtensions(extensions);
        }
        return submodelElement;
    }


    /**
     * Builds an AAS extension carrying the given EDC asset ID.
     *
     * @param assetId Asset id to embed in the extension.
     * @return AAS extension holding the asset id.
     */
    protected Extension buildExtension(String assetId) {
        return new DefaultExtension.Builder()
                .name(Asset.PROPERTY_ID)
                .value(assetId)
                .build();
    }


    /**
     * Top-down-search, bottom-up filtering of submodel elements. If at least one child of an otherwise to-be-removed
     * element needs to be kept, the element itself will not be removed. This element will be shown in the
     * self-description but will not have an EDC asset ID. This could compromise confidentiality in some cases,
     * self-description should be deactivated in that case.
     *
     * @param parent Reference of the parent element.
     * @param submodelElement Submodel element to filter.
     * @return The filtered submodel element, or {@code null} if it should be removed.
     */
    protected SubmodelElement filterSubmodelElementStructure(Reference parent, SubmodelElement submodelElement) {
        Reference submodelElementReference = AasUtils.toReference(parent, submodelElement);

        if (submodelElement instanceof SubmodelElementList list) {
            List<SubmodelElement> listChildren = list.getValue();
            List<SubmodelElement> filteredChildren = new ArrayList<>();
            // AASd-120 - aware
            for (int i = 0; i < listChildren.size(); i++) {
                SubmodelElement child = listChildren.get(i);
                child.setIdShort(String.valueOf(i));

                SubmodelElement filteredChild = filterSubmodelElementStructure(submodelElementReference, child);

                if (filteredChild != null) {
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

        if (client.eligibleForRegistration(AasUtils.toReference(parent, submodelElement)) ||
                submodelElement instanceof SubmodelElementList list && !list.getValue().isEmpty() ||
                submodelElement instanceof SubmodelElementCollection collection && !collection.getValue().isEmpty()) {
            return submodelElement;
        }
        return null;
    }
}
