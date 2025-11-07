package de.fraunhofer.iosb.app.handler.aas.registry;

import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.app.aas.mapper.referable.identifiable.IdentifiableMapper;
import de.fraunhofer.iosb.app.handler.RemoteHandler;
import de.fraunhofer.iosb.app.handler.aas.AasHandler;
import de.fraunhofer.iosb.app.handler.edc.EdcStoreHandler;
import de.fraunhofer.iosb.app.handler.util.DiffHelper;
import de.fraunhofer.iosb.client.exception.UnauthorizedException;
import de.fraunhofer.iosb.client.registry.AasRegistryClient;
import de.fraunhofer.iosb.model.context.registry.AasRegistryContext;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.util.AasUtils;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShellDescriptor;
import org.eclipse.digitaltwin.aas4j.v3.model.Descriptor;
import org.eclipse.digitaltwin.aas4j.v3.model.Endpoint;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Identifiable;
import org.eclipse.digitaltwin.aas4j.v3.model.ProtocolInformation;
import org.eclipse.digitaltwin.aas4j.v3.model.SecurityAttributeObject;
import org.eclipse.digitaltwin.aas4j.v3.model.SecurityTypeEnum;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelDescriptor;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShellDescriptor;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetInformation;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEnvironment;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultExtension;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodelDescriptor;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;

import java.net.ConnectException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.fraunhofer.iosb.constants.AasConstants.SUPPORTED_AAS_VERSION;
import static org.eclipse.edc.dataaddress.httpdata.spi.HttpDataAddressSchema.BASE_URL;

public class RemoteAasRegistryHandler extends AasHandler implements RemoteHandler {
    private static final List<String> SUPPORTED_PROTOCOLS = List.of("HTTP", "HTTPS", "http", "https");

    private final AasRegistryClient client;

    // This map keeps tabs on the current state of registered assets/contracts.
    // If an asset or its contract could not be registered, they will not appear in this map.
    // We keep this "cache" to not flood the Asset/ContractStores with requests.
    private final Map<PolicyBinding, Asset> referenceAssetMapping = new HashMap<>();

    public RemoteAasRegistryHandler(Monitor monitor, AasRegistryContext context, EdcStoreHandler edcStoreHandler) {
        super(monitor, context, edcStoreHandler);
        this.client = new AasRegistryClient(context);
        // initialize
        run();
    }

    @Override
    protected Environment getEnvironment() throws UnauthorizedException, ConnectException {
        List<AssetAdministrationShell> shells = List.of();
        List<Submodel> submodels = List.of();
        try {
            shells = getAllAsShells().values().stream().toList();
        } catch (NoEndpointException e) {
            monitor.severe(String.format("No endpoints for one shell descriptor at registry %s", getUri()), e);
        }
        try {
            submodels = getAllAsSubmodels().values().stream().toList();
        } catch (NoEndpointException e) {
            monitor.severe(String.format("No endpoints for one submodel descriptor at registry %s", getUri()), e);
        }

        return new DefaultEnvironment.Builder()
                .assetAdministrationShells(shells)
                .submodels(submodels)
                .build();
    }

    @Override
    public Environment buildSelfDescription() throws UnauthorizedException, ConnectException {
        return super.buildSelfDescription(this::registryIdentifiableMapper);
    }

    private @NotNull Identifiable registryIdentifiableMapper(Identifiable identifiable) {
        var ctx = new AasRegistryContext.Builder()
                .uri(referenceAssetMapping.entrySet().stream()
                        .filter(entry -> entry.getKey().referredElement().equals(AasUtils.toReference(identifiable)))
                        .map(Map.Entry::getValue)
                        .map(Asset::getDataAddress)
                        .map(da -> (String) da.getProperty(BASE_URL))
                        .filter(Objects::nonNull)
                        .map(URI::create)
                        .findAny()
                        .orElseThrow())
                .build();

        identifiable.getExtensions().add(new DefaultExtension.Builder()
                .name(Asset.PROPERTY_ID)
                .value(new IdentifiableMapper(ctx).generateId(AasUtils.toReference(identifiable)))
                .build());
        return identifiable;
    }

    @Override
    public void run() {
        if (!isAvailable()) {
            monitor.warning(String.format("%s unavailable", client.getUri()));
            return;
        }

        Map<PolicyBinding, Asset> filtered;
        try {
            filtered = filterMap();
        } catch (UnauthorizedException e) {
            throw new EdcException(String.format("Unauthorized exception when connecting to %s", getUri()), e);
        } catch (ConnectException e) {
            monitor.warning(String.format("Could not connect to %s", getUri()), e);
            return;
        }

        // All elements that are not currently registered (as far as we know)
        Map<PolicyBinding, Asset> toAdd = DiffHelper.getToAdd(referenceAssetMapping, filtered);
        // All elements that are currently registered (as far as we know) but should not
        Map<PolicyBinding, Asset> toRemove = DiffHelper.getToRemove(referenceAssetMapping, filtered);
        // All elements to update (policy bindings are not modifiable, thus not need to be checked)
        Map<PolicyBinding, Asset> toUpdate = DiffHelper.getToUpdate(referenceAssetMapping, filtered);

        toAdd.entrySet().stream()
                .filter(entry -> registerSingle(entry.getKey(), entry.getValue()).succeeded())
                .forEach(entry -> referenceAssetMapping.put(entry.getKey(), entry.getValue()));

        toRemove.entrySet().stream()
                .filter(entry -> unregisterSingle(entry.getKey(), entry.getValue()).succeeded())
                .forEach(entry -> referenceAssetMapping.remove(entry.getKey(), entry.getValue()));

        toUpdate.entrySet().stream()
                .filter(entry -> updateSingle(entry.getKey(), entry.getValue()).succeeded())
                .forEach(entry -> referenceAssetMapping.remove(entry.getKey(), entry.getValue()));
    }

    protected Map<PolicyBinding, Asset> filterMap() throws UnauthorizedException, ConnectException {

        Map<URI, AssetAdministrationShell> shells;
        Map<URI, Submodel> submodels;
        try {
            shells = getAllAsShells();
        } catch (NoEndpointException clientException) {
            monitor.warning(String.format("Could not get shell descriptors from registry %s", client.getUri()), clientException);
            shells = Map.of();
        }
        try {
            submodels = getAllAsSubmodels();
        } catch (NoEndpointException clientException) {
            monitor.warning(String.format("Could not get submodel descriptors from registry %s", client.getUri()), clientException);
            submodels = Map.of();
        }

        Map<PolicyBinding, Asset> shellAssets = shells.entrySet().stream()
                .map(this::mapEntry)
                .flatMap(policyBindingAssetMap -> policyBindingAssetMap.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<PolicyBinding, Asset> submodelAssets = submodels.entrySet().stream()
                .map(this::mapEntry)
                .flatMap(policyBindingAssetMap -> policyBindingAssetMap.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return Stream.of(shellAssets, submodelAssets)
                .flatMap(policyBindingAssetMap -> policyBindingAssetMap.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private <T extends Identifiable> Map<PolicyBinding, Asset> mapEntry(Map.Entry<URI, T> entry) {
        URI uri = entry.getKey();
        T entity = entry.getValue();

        Function<Identifiable, Asset> identifiableMappingFunction =
                new IdentifiableMapper(new AasRegistryContext.Builder().uri(uri).build())::map;
        return filterMapIdentifiable(entity, identifiableMappingFunction, submodelElementMapper::map);
    }

    @Override
    protected boolean isAvailable() {
        return client.isAvailable();
    }

    @Override
    protected URI getUri() {
        return client.getUri();
    }

    private Map<URI, Submodel> getAllAsSubmodels() throws UnauthorizedException, ConnectException, NoEndpointException {
        List<DefaultSubmodelDescriptor> submodelDescriptors = new ArrayList<>(client.getSubmodelDescriptors());

        client.getShellDescriptors().stream()
                .map(DefaultAssetAdministrationShellDescriptor::getSubmodelDescriptors)
                .flatMap(Collection::stream)
                .map(DefaultSubmodelDescriptor.class::cast)
                .forEach(submodelDescriptors::add);

        Map<URI, Submodel> collect = new HashMap<>();
        for (var descriptor : submodelDescriptors) {
            collect.put(getAppropriateEndpoint(descriptor.getEndpoints(), SubmodelDescriptor.class),
                    asSubmodel(descriptor));
        }
        return collect;
    }

    private Submodel asSubmodel(SubmodelDescriptor descriptor) {
        return new DefaultSubmodel.Builder()
                .administration(descriptor.getAdministration())
                .description(descriptor.getDescription())
                .displayName(descriptor.getDisplayName())
                .extensions(descriptor.getExtensions())
                .id(descriptor.getId())
                .idShort(descriptor.getIdShort())
                .semanticId(descriptor.getSemanticId())
                .supplementalSemanticIds(descriptor.getSupplementalSemanticId())
                .build();
    }

    private Map<URI, AssetAdministrationShell> getAllAsShells() throws UnauthorizedException, ConnectException, NoEndpointException {
        List<DefaultAssetAdministrationShellDescriptor> shellDescriptors = client.getShellDescriptors();
        Map<URI, AssetAdministrationShell> collect = new HashMap<>();
        for (var descriptor : shellDescriptors) {
            collect.put(getAppropriateEndpoint(descriptor.getEndpoints(), AssetAdministrationShellDescriptor.class),
                    asAssetAdministrationShell(descriptor));
        }
        return collect;
    }

    private AssetAdministrationShell asAssetAdministrationShell(AssetAdministrationShellDescriptor descriptor) {
        return new DefaultAssetAdministrationShell.Builder()
                .administration(descriptor.getAdministration())
                .assetInformation(new DefaultAssetInformation.Builder()
                        .assetKind(descriptor.getAssetKind())
                        .assetType(descriptor.getAssetType())
                        .globalAssetId(descriptor.getGlobalAssetId())
                        .specificAssetIds(descriptor.getSpecificAssetIds())
                        .build())
                .description(descriptor.getDescription())
                .displayName(descriptor.getDisplayName())
                .extensions(descriptor.getExtensions())
                .id(descriptor.getId())
                .idShort(descriptor.getIdShort())
                .build();
    }

    private <T extends Descriptor> URI getAppropriateEndpoint(List<Endpoint> endpoints, Class<T> clazz) throws NoEndpointException {
        List<String> interfaceShortNames;
        if (clazz.equals(AssetAdministrationShellDescriptor.class)) {
            interfaceShortNames = List.of("AAS-".concat(SUPPORTED_AAS_VERSION), "AAS-REPOSITORY-".concat(SUPPORTED_AAS_VERSION));
        } else if (clazz.equals(SubmodelDescriptor.class)) {
            interfaceShortNames = List.of("SUBMODEL-".concat(SUPPORTED_AAS_VERSION), "SUBMODEL-REPOSITORY-".concat(SUPPORTED_AAS_VERSION));
        } else {
            throw new IllegalArgumentException(String.format("Descriptor type not recognized: %s", clazz.getSimpleName()));
        }
        return endpoints.stream()
                .filter(ep -> interfaceShortNames.contains(ep.get_interface()))
                .map(Endpoint::getProtocolInformation)
                .filter(protocolInformation -> SUPPORTED_PROTOCOLS.contains(protocolInformation.getEndpointProtocol()))
                // Remove endpoints which require security
                .filter(protocolInformation ->
                        protocolInformation.getSecurityAttributes().isEmpty() ||
                                protocolInformation.getSecurityAttributes().stream()
                                        .map(SecurityAttributeObject::getType)
                                        .anyMatch(type -> type.equals(SecurityTypeEnum.NONE)))
                .map(ProtocolInformation::getHref)
                .map(URI::create)
                .findAny().orElseThrow(() -> new NoEndpointException("No suitable endpoints found."));
    }
}
