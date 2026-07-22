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
package de.fraunhofer.iosb.aas.dtr;

import de.fraunhofer.iosb.codec.Codec;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextSupplier;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.spi.EdcException;

import java.util.function.Supplier;

@Extension(value = DigitalTwinRegistryExtension.NAME)
public class DigitalTwinRegistryExtension implements ServiceExtension {
    public static final String NAME = "Digital Twin Registry Extension";

    @Inject
    private AssetIndex assetIndex;
    @Inject(required = false)
    private Codec codec;
    @Inject
    private ContractDefinitionStore contractDefinitionStore;
    @Inject(required = false)
    private SingleParticipantContextSupplier singleParticipantContextSupplier;
    @Inject(required = false)
    private PolicyDefinitionStore policyDefinitionStore;

    @Configuration
    private DigitalTwinRegistryExtensionConfiguration configuration;

    private DigitalTwinRegistryService digitalTwinRegistryService;

    @Override
    public void start() {
        Supplier<String> participantIdSupplier = () -> singleParticipantContextSupplier.get()
                .orElseThrow(msg -> new EdcException(msg.getFailureDetail())).getParticipantContextId();

        digitalTwinRegistryService = new DigitalTwinRegistryService(
                assetIndex,
                contractDefinitionStore,
                policyDefinitionStore,
                participantIdSupplier,
                codec,
                configuration);

        digitalTwinRegistryService.register();
    }

    @Override
    public void shutdown() {
        digitalTwinRegistryService.cleanUp();
    }

}
