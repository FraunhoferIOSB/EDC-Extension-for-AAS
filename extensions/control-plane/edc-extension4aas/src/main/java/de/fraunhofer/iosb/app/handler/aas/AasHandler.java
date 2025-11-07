package de.fraunhofer.iosb.app.handler.aas;

import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.app.aas.mapper.referable.SubmodelElementMapper;
import de.fraunhofer.iosb.app.aas.mapper.referable.identifiable.IdentifiableMapper;
import de.fraunhofer.iosb.app.handler.edc.EdcStoreHandler;
import de.fraunhofer.iosb.client.exception.UnauthorizedException;
import de.fraunhofer.iosb.model.context.AasServerContext;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.util.AasUtils;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
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
import org.eclipse.edc.spi.result.StoreResult;

import java.net.ConnectException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class AasHandler {

    protected final IdentifiableMapper identifiableMapper;
    protected final SubmodelElementMapper submodelElementMapper;
    protected final Monitor monitor;
    private final EdcStoreHandler edcStoreHandler;

    protected AasHandler(Monitor monitor, AasServerContext context,
                         EdcStoreHandler edcStoreHandler) {
        this.identifiableMapper = new IdentifiableMapper(context);
        this.submodelElementMapper = new SubmodelElementMapper(context);
        this.monitor = monitor;
        this.edcStoreHandler = edcStoreHandler;
    }

    protected abstract boolean isAvailable();

    protected abstract URI getUri();

    /* Initial population of EDC stores. */
    protected Map<PolicyBinding, Asset> initialize() {
        if (!isAvailable()) {
            return new HashMap<>();
        }

        Map<PolicyBinding, Asset> filtered;
        try {
            filtered = filterMap();
        } catch (UnauthorizedException | ConnectException e) {
            // TODO
            throw new RuntimeException(e);
        }

        long registered = filtered.entrySet().stream()
                .map(entry -> registerSingle(entry.getKey(), entry.getValue()))
                .filter(AbstractResult::succeeded)
                .count();

        monitor.info(String.format("Registered %s AAS elements from repository %s.", registered, getUri()));

        return filtered;
    }

    public void cleanUp() {
        monitor.info("Unregistering...");
        Map<PolicyBinding, Asset> filtered;
        try {
            filtered = filterMap();
        } catch (UnauthorizedException | ConnectException e) {
            throw new RuntimeException(e);
        }

        long unregistered = filtered.entrySet().stream()
                .map(entry -> unregisterSingle(entry.getKey(), entry.getValue()))
                .filter(AbstractResult::succeeded)
                .count();

        monitor.info(String.format("Unregistered %s AAS elements from repository %s.", unregistered, getUri()));
    }

    protected StoreResult<Void> registerSingle(PolicyBinding policyBinding, Asset asset) {
        return edcStoreHandler.register(policyBinding, asset);
    }

    protected StoreResult<Void> updateSingle(PolicyBinding policyBinding, Asset asset) {
        StoreResult<Asset> storeResultWithAsset = edcStoreHandler.update(asset);
        if (storeResultWithAsset.succeeded()) {
            return StoreResult.success();
        }

        String failureDetail = storeResultWithAsset.getFailureDetail();
        // note: if an update is called but the AssetIndex cannot find it, create the asset.
        return switch (storeResultWithAsset.reason()) {
            case NOT_FOUND -> registerSingle(policyBinding, asset);
            case ALREADY_EXISTS -> StoreResult.alreadyExists(failureDetail);
            case ALREADY_LEASED -> StoreResult.alreadyLeased(failureDetail);
            case DUPLICATE_KEYS -> StoreResult.duplicateKeys(failureDetail);
            case GENERAL_ERROR -> StoreResult.generalError(failureDetail);
        };
    }

    protected StoreResult<Void> unregisterSingle(PolicyBinding policyBinding, Asset asset) {
        return edcStoreHandler.unregister(policyBinding, asset.getId());
    }

    /**
     * Returns the self-description entity for this AAS.
     * The self-description is essentially a representation of this AAS preserving its structure, extended by EDC information for data space
     * consumers to get the necessary information to negotiate the data represented by an AAS element.
     *
     * @return The self-description (An AAS Environment with EDC information added as AAS.Extensions using the HasExtension property of an AAS
     *         referable).
     * @throws UnauthorizedException A call to the AAS was returned with a Status code of 401 or 403.
     * @throws ConnectException      A connection to the underlying AAS was unsuccessful.
     */
    public Environment buildSelfDescription() throws UnauthorizedException, ConnectException {
        Function<Identifiable, Identifiable> identifiableMapper = identifiable -> {
            if (!(identifiable instanceof Submodel) || referenceFilter().test(AasUtils.toReference(identifiable))) {
                identifiable.getExtensions().add(new DefaultExtension.Builder()
                        .name(Asset.PROPERTY_ID)
                        .value(this.identifiableMapper.generateId(AasUtils.toReference(identifiable)))
                        .build());
            }
            return identifiable;
        };

        return buildSelfDescription(identifiableMapper);
    }


    protected Environment buildSelfDescription(Function<Identifiable, Identifiable> identifiableMapper) throws UnauthorizedException,
            ConnectException {

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

    protected Map<PolicyBinding, Asset> filterMap() throws UnauthorizedException, ConnectException {
        Map<PolicyBinding, Asset> result = new HashMap<>();
        Environment environment = getEnvironment();
        environment.getAssetAdministrationShells().stream()
                .map(shell -> filterMapIdentifiable(shell, identifiableMapper::map, submodelElementMapper::map))
                .forEach(result::putAll);

        environment.getConceptDescriptions().stream()
                .map(conceptDescription -> filterMapIdentifiable(conceptDescription, identifiableMapper::map, submodelElementMapper::map))
                .forEach(result::putAll);

        environment.getSubmodels().stream()
                .map(submodel -> filterMapIdentifiable(submodel, identifiableMapper::map, submodelElementMapper::map))
                .forEach(result::putAll);

        return result;
    }


    protected <T> Map<PolicyBinding, T> filterMapIdentifiable(Identifiable identifiable,
                                                              Function<Identifiable, T> mappingFunction,
                                                              BiFunction<Reference, SubmodelElement, T> elementMappingFunction) {
        Map<PolicyBinding, T> filteredElements = new HashMap<>();

        Reference identifiableReference = AasUtils.toReference(identifiable);

        if (identifiable instanceof Submodel submodel) {
            submodel.getSubmodelElements().stream()
                    .map(element -> filterMapSubmodelElement(identifiableReference, element, elementMappingFunction))
                    .forEach(filteredElements::putAll);
        }

        Predicate<Reference> filter = referenceFilter();

        if (filter.test(identifiableReference)) {
            filteredElements.put(policyBindingOrDefault(identifiableReference), mappingFunction.apply(identifiable));
        }
        return filteredElements;
    }

    protected PolicyBinding policyBindingOrDefault(Reference identifiableReference) {
        return PolicyBinding.ofDefaults(identifiableReference);
    }

    private <T> Map<PolicyBinding, T> filterMapSubmodelElement(Reference parent, SubmodelElement submodelElement,
                                                               BiFunction<Reference, SubmodelElement, T> mappingFunction) {
        Map<PolicyBinding, T> result = new HashMap<>();

        Reference submodelElementReference = AasUtils.toReference(parent, submodelElement);

        if (submodelElement instanceof SubmodelElementList list) {
            List<SubmodelElement> children = list.getValue();
            // AASd-120 - aware
            for (int i = 0; i < children.size(); i++) {
                SubmodelElement child = children.get(i);
                child.setIdShort(String.valueOf(i));
                result.putAll(filterMapSubmodelElement(submodelElementReference, child, mappingFunction));
            }
        } else if (submodelElement instanceof SubmodelElementCollection collection) {
            collection.getValue().stream()
                    .map(child -> filterMapSubmodelElement(submodelElementReference, child, mappingFunction))
                    .forEach(result::putAll);
        }

        Predicate<Reference> filter = referenceFilter();

        if (filter.test(submodelElementReference)) {
            result.put(policyBindingOrDefault(submodelElementReference), mappingFunction.apply(parent, submodelElement));
        }

        return result;
    }


    protected SubmodelElement mapSubmodelElement(Reference reference, SubmodelElement submodelElement) {
        return submodelElement;
    }

    protected SubmodelElement filterSubmodelElementStructure(Reference reference, SubmodelElement submodelElement) {
        return submodelElement;
    }

    protected Predicate<Reference> referenceFilter() {
        return reference -> true;
    }

    protected Predicate<Identifiable> identifiableFilter() {
        return identifiable -> true;
    }

    /**
     * Returns the environment of the AAS server. This could be the environment of an AAS repository or the shell/submodel descriptors of an AAS
     * registry converted into an environment.
     *
     * @return The environment representing this AAS server
     * @throws UnauthorizedException A call to the AAS was returned with a Status code of 401 or 403.
     * @throws ConnectException      A connection to the underlying AAS was unsuccessful.
     */
    protected abstract Environment getEnvironment() throws UnauthorizedException, ConnectException;

    private record EnvironmentVisitor(Environment environment) {

        EnvironmentVisitor visitShells(Function<Identifiable, Identifiable> visitor) {
            environment.getAssetAdministrationShells().forEach(visitor::apply);
            return this;
        }

        EnvironmentVisitor visitShells(Predicate<Identifiable> eliminator) {
            environment.setAssetAdministrationShells(environment.getAssetAdministrationShells().stream().filter(eliminator).toList());
            return this;
        }

        EnvironmentVisitor visitConceptDescriptions(Function<Identifiable, Identifiable> visitor) {
            environment.getConceptDescriptions().forEach(visitor::apply);
            return this;
        }

        EnvironmentVisitor visitConceptDescriptions(Predicate<Identifiable> eliminator) {
            environment.setConceptDescriptions(environment.getConceptDescriptions().stream().filter(eliminator).toList());
            return this;
        }

        EnvironmentVisitor visitSubmodels(Function<Identifiable, Identifiable> visitor,
                                          BiFunction<Reference, SubmodelElement, SubmodelElement> childVisitor) {
            environment.getSubmodels().forEach(submodel ->
                    submodel.getSubmodelElements().forEach(submodelElement -> childVisitor.apply(AasUtils.toReference(submodel),
                            submodelElement)));
            environment.getSubmodels().forEach(visitor::apply);
            return this;
        }

        EnvironmentVisitor visitSubmodels(Predicate<Identifiable> eliminator,
                                          BiFunction<Reference, SubmodelElement, SubmodelElement> childEliminator) {
            environment.getSubmodels().forEach(submodel ->
                    submodel.setSubmodelElements(
                            submodel.getSubmodelElements().stream()
                                    .map(submodelElement ->
                                            childEliminator.apply(AasUtils.toReference(submodel), submodelElement))
                                    .filter(Objects::nonNull)
                                    .toList()));

            environment.setSubmodels(environment.getSubmodels().stream().filter(eliminator).toList());
            return this;
        }
    }
}
