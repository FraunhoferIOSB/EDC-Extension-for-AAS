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

import de.fraunhofer.iosb.app.controller.AasController;
import de.fraunhofer.iosb.app.controller.ResourceController;
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository;
import de.fraunhofer.iosb.app.sync.Synchronizer;

import java.net.URL;

import static de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository.SelfDescriptionSourceType;
import static de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository.SelfDescriptionSourceType.REGISTRY;

/**
 * Synchronizer for AAS registry
 */
public class RegistrySynchronizer extends Synchronizer {

    public RegistrySynchronizer(SelfDescriptionRepository selfDescriptionRepository,
                                ResourceController resourceController,
                                AasController aasController) {
        super(selfDescriptionRepository, resourceController, aasController);
    }

    @Override
    public SelfDescriptionSourceType supportedType() {
        return REGISTRY;
    }

    @Override
    public void synchronize(URL aasServiceUrl) {
        // TODO
        throw new UnsupportedOperationException("Synchronize for registries not implemented yet");
    }
}
