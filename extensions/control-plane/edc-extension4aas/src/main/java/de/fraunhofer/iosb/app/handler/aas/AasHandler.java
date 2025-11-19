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
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Identifiable;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultExtension;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.StoreFailure;
import org.eclipse.edc.spi.result.StoreResult;
import org.jetbrains.annotations.NotNull;

import java.net.ConnectException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;


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
                .filter(entry -> referenceFilter().test(entry.getKey()))
                .map(entry -> Map.entry(policyBindingFor(entry.getKey()), entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }


    protected @NotNull Predicate<Reference> referenceFilter() {
        List<Reference> references = client.getReferences();

        return (ref) -> references.isEmpty() || references.contains(ref);
    }


    /* Initial population of EDC stores. */
    protected Map<PolicyBinding, Asset> initialize() throws UnauthorizedException, ConnectException {
        Environment currentEnvironment = getEnvironment();

        Map<Reference, Asset> mapped = MappingHelper.map(currentEnvironment, identifiableMapper::map, submodelElementMapper::map);

        Map<PolicyBinding, Asset> filtered = mapped.entrySet().stream()
                .filter(entry -> referenceFilter().test(entry.getKey()))
                .map(entry -> Map.entry(policyBindingFor(entry.getKey()), entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        long registered = filtered.entrySet().stream()
                .map(entry -> registerSingle(entry.getKey(), entry.getValue()))
                .filter(AbstractResult::succeeded)
                .count();

        monitor.info(String.format("Registered %s AAS elements from repository %s.", registered, client.getUri()));

        return filtered;
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
            if (identifiableFilter().test(identifiable)) {
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
            if (!(identifiable instanceof Submodel) || referenceFilter().test(AasUtils.toReference(identifiable))) {
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


    protected abstract SubmodelElement mapSubmodelElement(Reference reference, SubmodelElement submodelElement);


    protected abstract SubmodelElement filterSubmodelElementStructure(Reference reference, SubmodelElement submodelElement);


    protected Predicate<Identifiable> identifiableFilter() {
        List<Reference> references = client.getReferences();

        return identifiable -> references.isEmpty() || references.contains(AasUtils.toReference(identifiable));
    }
}
