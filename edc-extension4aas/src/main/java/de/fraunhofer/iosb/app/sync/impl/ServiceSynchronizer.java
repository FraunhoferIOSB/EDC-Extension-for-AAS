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
package de.fraunhofer.iosb.app.sync.impl;

import de.fraunhofer.iosb.app.aas.EnvironmentToAssetMapper;
import de.fraunhofer.iosb.app.controller.AasController;
import de.fraunhofer.iosb.app.controller.ResourceController;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository;
import de.fraunhofer.iosb.app.sync.Synchronizer;
import de.fraunhofer.iosb.app.util.AssetUtil;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Optional;

import static de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository.SelfDescriptionSourceType;
import static de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository.SelfDescriptionSourceType.SERVICE;
import static java.lang.String.format;

/**
 * Synchronize registered AAS services with local
 * self-descriptions and assetIndex/contractStore.
 */
public class ServiceSynchronizer extends Synchronizer {

    ServiceSynchronizer(SelfDescriptionRepository selfDescriptionRepository,
                        AasController aasController,
                        AssetIndex assetIndex,
                        ContractDefinitionStore contractStore,
                        PolicyDefinitionStore policyStore,
                        Monitor monitor) {
        super(selfDescriptionRepository,
                new ResourceController(assetIndex, contractStore, policyStore, monitor),
                aasController);
    }

    @Override
    public SelfDescriptionSourceType supportedType() {
        return SERVICE;
    }

    @Override
    public void synchronize(URL aasServiceUrl) {
        EnvironmentToAssetMapper mapper = new EnvironmentToAssetMapper(aasServiceUrl,
                Configuration.getInstance().isOnlySubmodels());

        var currentEnvironment = Optional
                .ofNullable(selfDescriptionRepository.getSelfDescription(aasServiceUrl))
                .orElse(Asset.Builder.newInstance().build());

        var newEnvironment = getCurrentAasEnvironment(aasServiceUrl);

        var newEnvironmentAsset = mapper.map(newEnvironment);

        syncEnvironment(AssetUtil.flatMapAssets(newEnvironmentAsset),
                AssetUtil.flatMapAssets(currentEnvironment));

        selfDescriptionRepository.updateSelfDescription(aasServiceUrl, newEnvironmentAsset);
    }

    private Environment getCurrentAasEnvironment(URL aasServiceUrl) {
        Environment newEnvironment;

        try { // Fetch current AAS model from AAS service
            newEnvironment = aasController.getAasModelWithUrls(aasServiceUrl);
        } catch (IOException aasServiceUnreachableException) {
            throw new EdcException(format("Could not reach AAS service (%s): %s", aasServiceUrl,
                    aasServiceUnreachableException.getMessage()), aasServiceUnreachableException);
        }
        return newEnvironment;
    }

    private void syncEnvironment(Collection<Asset> newEnvironment,
                                 Collection<Asset> oldEnvironment) {
        oldEnvironment.stream()
                .filter(oldShell -> !newEnvironment.contains(oldShell))
                .forEach(deleted -> resourceController.deleteAssetAndContracts(deleted.getId()));

        resourceController.createResources(newEnvironment.stream().filter(oldShell -> !oldEnvironment.contains(oldShell)).toList());
    }

    public static class Builder {
        private SelfDescriptionRepository selfDescriptionRepository;
        private AasController aasController;
        private AssetIndex assetIndex;
        private ContractDefinitionStore contractStore;
        private PolicyDefinitionStore policyStore;
        private Monitor monitor;

        private Builder() {
        }

        public static Builder getInstance() {
            return new Builder();
        }

        public Builder selfDescriptionRepository(SelfDescriptionRepository selfDescriptionRepository) {
            this.selfDescriptionRepository = selfDescriptionRepository;
            return this;
        }

        public Builder aasController(AasController aasController) {
            this.aasController = aasController;
            return this;
        }

        public Builder assetIndex(AssetIndex assetIndex) {
            this.assetIndex = assetIndex;
            return this;
        }

        public Builder contractStore(ContractDefinitionStore contractStore) {
            this.contractStore = contractStore;
            return this;
        }

        public Builder policyStore(PolicyDefinitionStore policyStore) {
            this.policyStore = policyStore;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            this.monitor = monitor;
            return this;
        }

        public ServiceSynchronizer build() {
            return new ServiceSynchronizer(selfDescriptionRepository, aasController, assetIndex, contractStore,
                    policyStore, monitor);
        }
    }
}
