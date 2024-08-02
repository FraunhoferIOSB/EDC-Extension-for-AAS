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
package de.fraunhofer.iosb.app.model.ids;

import de.fraunhofer.iosb.app.pipeline.PipelineResult;
import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * TODO correct package?
 */
public class SelfDescriptionUpdater extends PipelineStep<Map<String, Asset>, Map<Asset, Asset>> {

    private final SelfDescriptionRepository selfDescriptionRepository;

    public SelfDescriptionUpdater(SelfDescriptionRepository selfDescriptionRepository) {
        this.selfDescriptionRepository = selfDescriptionRepository;
    }

    /**
     * Checks new Assets into self-description repository
     *
     * @param registered URL and asset for each registered AAS service
     * @return ID of old asset and new asset for synchronizer to create changeSet
     */
    @Override
    public PipelineResult<Map<Asset, Asset>> execute(Map<String, Asset> registered) throws Exception {
        Map<Asset, Asset> result = new HashMap<>();

        for (var entry : registered.entrySet()) {
            var existingAsset = Optional.ofNullable(selfDescriptionRepository.getSelfDescriptionAsset(entry.getKey())).orElse(Asset.Builder.newInstance().build());
            result.put(existingAsset, entry.getValue());
            selfDescriptionRepository.updateSelfDescription(entry.getKey(), entry.getValue());
        }

        return PipelineResult.success(result);
    }
}
