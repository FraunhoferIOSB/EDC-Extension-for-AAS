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
package de.fraunhofer.iosb.app.edc.asset;

import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.spi.EdcException;

public class AssetRemover extends PipelineStep<String, Void> {

    private final AssetIndex assetIndex;

    public AssetRemover(AssetIndex assetIndex) {
        this.assetIndex = assetIndex;
    }

    /**
     * Removes asset from assetIndex.
     *
     * @param assetId asset id
     */
    @Override
    public Void execute(String assetId) throws Exception {
        var storeResult = assetIndex.deleteById(assetId);
        if (storeResult.succeeded()) {
            return null;
        } else {
            throw new EdcException("Could not delete asset %s. %s".formatted(assetId, storeResult.getFailure().getMessages()));
        }
    }


}
