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
package de.fraunhofer.iosb.ilt.dataspace.app.controller.dto;

import java.net.URI;
import java.util.List;
import java.util.Map;


/**
 * A list of references and asset ids for assets that were created during the process of registering an AAS server.
 *
 * @param registeredAssets The asset IDs.
 */
public record RegisteredAssetIdsDTO(URI aasRepositoryUrl, List<Map.Entry<String, String>> registeredAssets) {}
