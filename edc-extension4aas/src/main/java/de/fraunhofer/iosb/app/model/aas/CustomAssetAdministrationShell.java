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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetInformation;

import java.util.Objects;

/**
 * AAS Model for the self-description of the edc
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonAutoDetect
public class CustomAssetAdministrationShell extends AssetAdministrationShellElement {

    protected String id;
    protected String idShort;
    protected AssetInformation assetInformation;

    public static CustomAssetAdministrationShell fromAssetAdministrationShell(AssetAdministrationShell from) {
        final var result = new CustomAssetAdministrationShell();

        result.setIdShort(from.getIdShort());
        result.setId(from.getId());
        result.setAssetInformation(from.getAssetInformation());

        return result;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdShort() {
        return idShort;
    }

    public void setIdShort(String idShort) {
        this.idShort = idShort;
    }

    public AssetInformation getAssetInformation() {
        return assetInformation;
    }

    public void setAssetInformation(AssetInformation assetInformation) {
        this.assetInformation = assetInformation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, idShort, assetInformation);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomAssetAdministrationShell that = (CustomAssetAdministrationShell) o;

        return Objects.equals(id, that.id) &&
                Objects.equals(idShort, that.idShort) &&
                Objects.equals(assetInformation, that.assetInformation);
    }

}
