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
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.connector.controlplane.catalog.spi.Distribution;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DISTRIBUTION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_ATTRIBUTE;


/**
 * Transform JSON to Dataset.
 *
 * @see JsonObjectToCatalogTransformer explanation as to why this is needed here.
 */
public class JsonObjectToDatasetTransformer extends AbstractJsonLdTransformer<JsonObject, Dataset> {

    public JsonObjectToDatasetTransformer() {
        super(JsonObject.class, Dataset.class);
    }

    @Override
    public @Nullable Dataset transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var builder = Dataset.Builder.newInstance();

        // Collect parameters
        builder.id(nodeId(jsonObject));
        transformArrayOrObject(jsonObject.get(DCAT_DISTRIBUTION_ATTRIBUTE), Distribution.class, builder::distribution, context);
        jsonObject.getJsonArray(ODRL_POLICY_ATTRIBUTE)
                .forEach(offer -> builder.offer(nodeId(offer), transformObject(offer, Policy.class, context)));

        // Note: properties not considered (see CatalogTransformer)
        return builderResult(builder::build, context);
    }
}
