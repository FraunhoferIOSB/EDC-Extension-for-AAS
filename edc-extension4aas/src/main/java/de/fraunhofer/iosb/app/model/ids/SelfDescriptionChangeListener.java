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

import java.net.URL;

public interface SelfDescriptionChangeListener {

    /**
     * Called when a self description was created
     *
     * @param aasUrl The URL of the newly registered AAS service
     */
    void created(URL aasUrl);

    /**
     * Called when a self description was removed
     *
     * @param removed The SelfDescription of the unregistered AAS service
     */
    void removed(SelfDescription removed);

}
