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

import de.fraunhofer.iosb.app.model.aas.service.Service;
import de.fraunhofer.iosb.app.model.aas.registry.Registry;

public interface SelfDescriptionChangeListener {

    /**
     * Called when a registry was created
     *
     * @param registry Newly created registry Newly
     */
    default void created(Registry registry) {
    }

    /**
     * Called when a service was created
     *
     * @param service Newly created service
     */
    default void created(Service service) {
    }

    /**
     * Called when a self-description was removed
     *
     * @param service Service that was removed
     */
    void removed(Service service);

    /**
     * Called when a self-description was removed
     *
     * @param registry Registry that was removed
     */
    void removed(Registry registry);

}
