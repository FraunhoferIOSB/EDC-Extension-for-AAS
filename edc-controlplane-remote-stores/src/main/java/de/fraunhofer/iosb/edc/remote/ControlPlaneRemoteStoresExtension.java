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
package de.fraunhofer.iosb.edc.remote;

import de.fraunhofer.iosb.edc.remote.stores.asset.RemoteAssetIndex;
import de.fraunhofer.iosb.edc.remote.stores.contract.RemoteContractDefinitionStore;
import de.fraunhofer.iosb.edc.remote.stores.policy.RemotePolicyDefinitionStore;
import de.fraunhofer.iosb.edc.remote.transform.JsonObjectFromContractDefinitionTransformer;
import de.fraunhofer.iosb.edc.remote.transform.JsonObjectFromPolicyDefinitionTransformer;
import de.fraunhofer.iosb.edc.remote.transform.JsonObjectToContractDefinitionTransformer;
import de.fraunhofer.iosb.edc.remote.transform.JsonObjectToPolicyDefinitionTransformer;
import jakarta.json.Json;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.connector.controlplane.transform.edc.from.JsonObjectFromAssetTransformer;
import org.eclipse.edc.connector.controlplane.transform.odrl.OdrlTransformersFactory;
import org.eclipse.edc.connector.controlplane.transform.odrl.from.JsonObjectFromPolicyTransformer;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.participant.spi.ParticipantIdMapper;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromCriterionTransformer;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromDataAddressTransformer;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromQuerySpecTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToCriterionTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToDataAddressTransformer;

import java.util.Map;

import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Provides({ AssetIndex.class, PolicyDefinitionStore.class, ContractDefinitionStore.class })
@Extension(value = ControlPlaneRemoteStoresExtension.NAME)
public class ControlPlaneRemoteStoresExtension implements ServiceExtension {

    public static final String NAME = "Remote Control-Plane Store Accessors";
    public static final String MGMT_API = "edc.controlplane.management.";
    public static final String CONTROL_PLANE = "edc.controlplane.";
    public static final String VERSION_API = "edc.controlplane.version.";


    @Setting(description = "Remote control plane full management API URL", key = MGMT_API + "url", required = false)
    private String managementFullUrl;


    @Setting(description = "Remote control-plane protocol", key = CONTROL_PLANE + "protocol", required = false, defaultValue = "http")
    private String protocol;

    @Setting(description = "Remote control-plane hostname", key = CONTROL_PLANE + "hostname", required = false, defaultValue = "localhost")
    private String hostname;

    @Setting(description = "Remote control-plane management API path", key = MGMT_API + "path",
            required = false, defaultValue = "/management")
    private String managementPath;

    @Setting(description = "Remote control-plane management API port", key = MGMT_API + "port",
            required = false, defaultValue = "8182")
    private int managementPort;


    @Setting(description = "Version API port (Only used if " + MGMT_API + "url not defined)", key = VERSION_API + "port",
            required = false, defaultValue = "7171")
    private int versionPort;

    @Setting(description = "Version API path (Only used if \" + MGMT_API + \"url not defined)", key = VERSION_API +
            "path", required = false, defaultValue = "/.well-known/api/v1/version")
    private String versionPath;

    @Inject
    private EdcHttpClient edcHttpClient;
    @Inject
    private JsonLd jsonLd;
    @Inject
    private ParticipantIdMapper participantIdMapper;
    @Inject
    private TypeManager typeManager;
    @Inject
    private TypeTransformerRegistry typeTransformerRegistry;

    @Override
    public void initialize(ServiceExtensionContext context) {
        registerTransformers();
    }

    @Provider
    public AssetIndex provideAssetIndex(ServiceExtensionContext context) {
        return new RemoteAssetIndex.Builder()
                .httpClient(edcHttpClient)
                .monitor(context.getMonitor())
                .typeManager(typeManager)
                .typeTransformerRegistry(typeTransformerRegistry)
                .jsonLd(jsonLd)
                .fullManagementUrl(managementFullUrl)
                .protocol(protocol)
                .hostname(hostname)
                .managementPort(managementPort)
                .managementPath(managementPath)
                .versionPort(versionPort)
                .versionPath(versionPath)
                .build();
        //context.registerService(DataAddressResolver.class, assetIndex);
    }

    @Provider
    public PolicyDefinitionStore providePolicyDefinitionStore(ServiceExtensionContext context) {
        return new RemotePolicyDefinitionStore.Builder()
                .httpClient(edcHttpClient)
                .monitor(context.getMonitor())
                .typeManager(typeManager)
                .typeTransformerRegistry(typeTransformerRegistry)
                .jsonLd(jsonLd)
                .fullManagementUrl(managementFullUrl)
                .protocol(protocol)
                .hostname(hostname)
                .managementPort(managementPort)
                .managementPath(managementPath)
                .versionPort(versionPort)
                .versionPath(versionPath)
                .build();
    }

    @Provider
    public ContractDefinitionStore provideContractDefinitionStore(ServiceExtensionContext context) {
        return new RemoteContractDefinitionStore.Builder()
                .httpClient(edcHttpClient)
                .monitor(context.getMonitor())
                .typeManager(typeManager)
                .typeTransformerRegistry(typeTransformerRegistry)
                .jsonLd(jsonLd)
                .fullManagementUrl(managementFullUrl)
                .protocol(protocol)
                .hostname(hostname)
                .managementPort(managementPort)
                .managementPath(managementPath)
                .versionPort(versionPort)
                .versionPath(versionPath)
                .build();
    }

    private void registerTransformers() {
        var jsonFactory = Json.createBuilderFactory(Map.of());
        // Register (de)serializers
        OdrlTransformersFactory.jsonObjectToOdrlTransformers(participantIdMapper)
                .forEach(typeTransformerRegistry::register);
        typeTransformerRegistry.register(new JsonObjectFromPolicyTransformer(jsonFactory, participantIdMapper));
        typeTransformerRegistry.register(new JsonObjectFromQuerySpecTransformer(jsonFactory));
        typeTransformerRegistry.register(new JsonObjectFromAssetTransformer(jsonFactory, typeManager, JSON_LD));
        typeTransformerRegistry.register(new JsonObjectFromDataAddressTransformer(jsonFactory, typeManager, JSON_LD));
        typeTransformerRegistry.register(new JsonObjectToDataAddressTransformer());
        typeTransformerRegistry.register(new JsonObjectToPolicyDefinitionTransformer());
        typeTransformerRegistry.register(new JsonObjectFromPolicyDefinitionTransformer(jsonFactory, typeManager, JSON_LD));
        typeTransformerRegistry.register(new JsonObjectFromContractDefinitionTransformer(jsonFactory, typeManager, JSON_LD));
        typeTransformerRegistry.register(new JsonObjectToContractDefinitionTransformer());
        typeTransformerRegistry.register(new JsonObjectFromCriterionTransformer(jsonFactory, typeManager, JSON_LD));
        typeTransformerRegistry.register(new JsonObjectToCriterionTransformer());
    }
}
