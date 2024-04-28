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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.digitaltwin.aas4j.v3.model.EmbeddedDataSpecification;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;

import java.util.List;

/*
 * Collect common attributes of every AAS element.
 */
public class AssetAdministrationShellElement extends IdsAssetElement {

    protected String idShort;

    @JsonProperty("semanticId")
    protected Reference customSemanticId;

    protected List<EmbeddedDataSpecification> embeddedDataSpecifications;

    public String getIdShort() {
        return idShort;
    }

    public void setIdShort(String idShort) {
        this.idShort = idShort;
    }

    public Reference getSemanticId() {
        return customSemanticId;
    }

    public void setSemanticId(Reference semanticId) {
        this.customSemanticId = semanticId;
    }

    public List<EmbeddedDataSpecification> getEmbeddedDataSpecifications() {
        return embeddedDataSpecifications;
    }

    public void setEmbeddedDataSpecifications(List<EmbeddedDataSpecification> embeddedDataSpecifications) {
        this.embeddedDataSpecifications = embeddedDataSpecifications;
    }
}
