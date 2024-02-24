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
package de.fraunhofer.iosb.client.policy;

import de.fraunhofer.iosb.client.util.Pair;
import org.eclipse.edc.catalog.spi.Dataset;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.spi.catalog.CatalogService;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.net.URL;
import java.util.List;
import java.util.Optional;

/**
 * Provides API for accepted policy management and provider dataset retrieval.
 * For documentation see {@link de.fraunhofer.iosb.client.ClientEndpoint}
 */
public class PolicyController {


    private final PolicyDefinitionStore policyDefinitionStore;
    private final PolicyService policyService;

    public PolicyController(Monitor monitor, CatalogService catalogService,
                            TypeTransformerRegistry typeTransformerRegistry, Config systemConfig) {
        var config = new PolicyServiceConfig(systemConfig);

        this.policyDefinitionStore = new PolicyDefinitionStore(monitor, config.getAcceptedPolicyDefinitionsPath());
        this.policyService = new PolicyService(catalogService, typeTransformerRegistry, config,
                this.policyDefinitionStore);
    }

    public Dataset getDataset(String counterPartyId, URL counterPartyUrl, String assetId) throws InterruptedException {
        return policyService.getDatasetForAssetId(counterPartyId, counterPartyUrl, assetId);
    }

    /**
     * Return policyDefinition for assetId that match any policyDefinitions' policy
     * of
     * the services' policyDefinitionStore instance containing user added
     * policyDefinitions.
     * If more than one policyDefinitions are provided by the provider
     * connector, an AmbiguousOrNullException will be thrown.
     *
     * @param counterPartyId  Provider of the asset. (id)
     * @param counterPartyUrl Provider of the asset. (url)
     * @param assetId         Asset ID of the asset whose contract should be fetched.
     * @return One policyDefinition offered by the provider for the given assetId.
     * @throws InterruptedException Thread for agreementId was waiting, sleeping, or
     *                              otherwise occupied, and was
     *                              interrupted.
     */
    public Pair<String, Policy> getAcceptablePolicyForAssetId(String counterPartyId, URL counterPartyUrl, String assetId)
            throws InterruptedException {
        return policyService.getAcceptablePolicyForAssetId(counterPartyId, counterPartyUrl, assetId);
    }

    public void addAcceptedPolicyDefinitions(PolicyDefinition[] policyDefinitions) {
        policyDefinitionStore.putPolicyDefinitions(policyDefinitions);
    }

    public List<PolicyDefinition> getAcceptedPolicyDefinitions() {
        return policyDefinitionStore.getPolicyDefinitions();
    }

    public Optional<PolicyDefinition> deleteAcceptedPolicyDefinition(String policyDefinitionId) {
        return policyDefinitionStore.removePolicyDefinition(policyDefinitionId);
    }

    public Optional<PolicyDefinition> updateAcceptedPolicyDefinition(PolicyDefinition policyDefinition) {
        return policyDefinitionStore.updatePolicyDefinitions(policyDefinition);
    }

}
