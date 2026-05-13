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

import de.fraunhofer.iosb.aas.lib.auth.AuthenticationMethod;
import de.fraunhofer.iosb.aas.lib.auth.impl.ApiKey;
import de.fraunhofer.iosb.aas.lib.auth.impl.NoAuth;
import de.fraunhofer.iosb.edc.remote.stores.asset.RemoteAssetIndex;
import de.fraunhofer.iosb.edc.remote.stores.contract.RemoteContractDefinitionStore;
import de.fraunhofer.iosb.edc.remote.stores.policy.RemotePolicyDefinitionStore;
import de.fraunhofer.iosb.codec.Codec;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.util.Optional;


@Provides({
        AssetIndex.class,
        PolicyDefinitionStore.class,
        ContractDefinitionStore.class
})
@Extension(value = EdcConnectorClientExtension.NAME)
public class EdcConnectorClientExtension implements ServiceExtension {

    public static final String NAME = "Remote Control-Plane Store Accessors";
    public static final String CONTROL_PLANE = "edc.controlplane.";
    public static final String MGMT_API = "edc.controlplane.management.";

    @Setting(description = "Remote control plane full management API URL", key = MGMT_API + "url")
    private String managementUri;

    @Setting(description = "Remote control-plane API Key", key = CONTROL_PLANE + "auth.key", required = false)
    private String apiKey;

    @Setting(description = "Remote control-plane vault secret alias for authentication. Takes precedence over raw key", key = CONTROL_PLANE + "auth.key.alias", required = false)
    private String apiKeyAlias;

    @Inject
    private Codec codec;
    @Inject
    private EdcHttpClient edcHttpClient;
    @Inject
    private Vault vault;

    private AuthenticationMethod authenticationMethod;


    @Override
    public void initialize(ServiceExtensionContext context) {
        authenticationMethod =
                Optional.ofNullable(apiKeyAlias)
                        .map(alias -> (AuthenticationMethod) new ApiKey("x-api-key", alias))
                        .or(() -> Optional.ofNullable(apiKey)
                                .map(k -> (AuthenticationMethod) new ApiKey("x-api-key", k, vault)))
                        .orElseGet(NoAuth::new);
    }


    @Provider
    public AssetIndex provideAssetIndex(ServiceExtensionContext context) {
        return new RemoteAssetIndex.Builder()
                .monitor(context.getMonitor())
                .httpClient(edcHttpClient)
                .managementUri(managementUri)
                .authenticationMethod(authenticationMethod)
                .codec(codec)
                .vault(vault)
                .build();
    }


    @Provider
    public PolicyDefinitionStore providePolicyDefinitionStore(ServiceExtensionContext context) {
        return new RemotePolicyDefinitionStore.Builder()
                .monitor(context.getMonitor())
                .httpClient(edcHttpClient)
                .managementUri(managementUri)
                .authenticationMethod(authenticationMethod)
                .codec(codec)
                .vault(vault)
                .build();
    }


    @Provider
    public ContractDefinitionStore provideContractDefinitionStore(ServiceExtensionContext context) {
        return new RemoteContractDefinitionStore.Builder()
                .monitor(context.getMonitor())
                .httpClient(edcHttpClient)
                .managementUri(managementUri)
                .authenticationMethod(authenticationMethod)
                .codec(codec)
                .vault(vault)
                .build();
    }
}
