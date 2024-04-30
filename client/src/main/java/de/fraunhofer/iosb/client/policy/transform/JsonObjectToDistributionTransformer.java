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
package de.fraunhofer.iosb.client.policy.transform;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataService;
import org.eclipse.edc.connector.controlplane.catalog.spi.Distribution;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DATA_SERVICE_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCT_FORMAT_ATTRIBUTE;

/**
 * Transform JSON to DataService.
 *
 * @see JsonObjectToCatalogTransformer explanation as to why this is needed here.
 */
public class JsonObjectToDistributionTransformer extends AbstractJsonLdTransformer<JsonObject, Distribution> {

    public JsonObjectToDistributionTransformer() {
        super(JsonObject.class, Distribution.class);
    }

    @Override
    public @Nullable Distribution transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var builder = Distribution.Builder.newInstance();

        transformMandatoryString(jsonObject.get(DCT_FORMAT_ATTRIBUTE), builder::format, context);
        transformArrayOrObject(jsonObject.get(DCAT_DATA_SERVICE_ATTRIBUTE), DataService.class, builder::dataService, context);

        return builderResult(builder::build, context);
    }
}
