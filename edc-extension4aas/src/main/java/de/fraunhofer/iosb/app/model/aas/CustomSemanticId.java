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
package de.fraunhofer.iosb.app.model.aas;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.adminshell.aas.v3.model.Key;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JsonAutoDetect
@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonInclude(Include.NON_NULL)
public class CustomSemanticId {
    private List<CustomSemanticIdKey> keys;

    public List<CustomSemanticIdKey> getKeys() {
        return keys;
    }

    public void setKeys(List<CustomSemanticIdKey> keys) {
        this.keys = keys;
    }

    public CustomSemanticId() {
        this.keys = Collections.emptyList();
    }

    public CustomSemanticId(List<Key> keys) {
        this.keys = new ArrayList<>();
        for (Key key : keys) {

            var customSemanticIdKey = new CustomSemanticIdKey();

            customSemanticIdKey.setIdType(key.getIdType());
            customSemanticIdKey.setType(key.getType());
            customSemanticIdKey.setValue(key.getValue());

            this.keys.add(customSemanticIdKey);

        }
    }

}
