/*
 * Copyright (c) 2021 Fraunhofer IOSB, eine rechtlich nicht selbstaendige
 * Einrichtung der Fraunhofer-Gesellschaft zur Foerderung der angewandten
 * Forschung e.V.
 * Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
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
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataService;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DATASET_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DATA_SERVICE_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DSPACE_PROPERTY_PARTICIPANT_ID;

/**
 * Transformer for Catalog in JSON representation to EDC Catalog class.
 * Modified code from <a href="https://github.com/eclipse-edc/Connector/tree/v0.5.1/core/common/transform-core">github.com</a>
 * <br>See also: <a href="https://github.com/eclipse-edc/Connector/pull/3916">https://github.com/eclipse-edc/Connector/pull/3916</a>
 */
public class JsonObjectToCatalogTransformer extends AbstractJsonLdTransformer<JsonObject, Catalog> {

    public JsonObjectToCatalogTransformer() {
        super(JsonObject.class, Catalog.class);
    }

    @Override
    public @Nullable Catalog transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var builder = Catalog.Builder.newInstance();

        // Collect parameters
        builder.id(nodeId(jsonObject));
        transformString(jsonObject.get(DSPACE_PROPERTY_PARTICIPANT_ID), builder::participantId, context);
        transformArrayOrObject(jsonObject.get(DCAT_DATASET_ATTRIBUTE), Dataset.class, builder::dataset, context);
        transformArrayOrObject(jsonObject.get(DCAT_DATA_SERVICE_ATTRIBUTE), DataService.class, builder::dataService, context);

        // Note: We don't need additional properties here, so they are ignored
        return builderResult(builder::build, context);
    }
}
