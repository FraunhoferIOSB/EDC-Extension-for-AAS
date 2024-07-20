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
package de.fraunhofer.iosb.app.sync;

import de.fraunhofer.iosb.app.controller.AasController;
import de.fraunhofer.iosb.app.controller.ResourceController;
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository;
import de.fraunhofer.iosb.app.util.AssetUtil;

import java.net.URL;

import static de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository.SelfDescriptionSourceType;


/**
 * Synchronize registered AAS services with local
 * self-descriptions and assetIndex/contractStore.
 */
public abstract class Synchronizer {

    protected final SelfDescriptionRepository selfDescriptionRepository;
    protected final ResourceController resourceController;
    protected final AasController aasController;

    protected Synchronizer(SelfDescriptionRepository selfDescriptionRepository, ResourceController resourceController,
                           AasController aasController) {
        this.selfDescriptionRepository = selfDescriptionRepository;
        this.resourceController = resourceController;
        this.aasController = aasController;
    }

    public abstract SelfDescriptionSourceType supportedType();

    public abstract void synchronize(URL aasServiceUrl);

    void remove(URL toRemove) {
        AssetUtil.flatMapAssets(selfDescriptionRepository.getSelfDescription(toRemove))
                .forEach(element -> resourceController.deleteAssetAndContracts(element.getId()));
    }
}
