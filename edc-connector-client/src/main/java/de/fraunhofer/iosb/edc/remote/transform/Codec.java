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
package de.fraunhofer.iosb.edc.remote.transform;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.entity.Entity;
import org.eclipse.edc.spi.result.Failure;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.NotNull;

import java.io.StringReader;
import java.util.List;

public class Codec {

    private static final String SERIALIZATION_ERROR = "Failed serializing %s: %s";
    private static final String DESERIALIZATION_ERROR = "Failed deserializing %s: %s";
    private static final String COMPACTION_ERROR = "Failed compacting json-ld %s: %s";
    private static final String EXPANSION_ERROR = "Failed expanding json-ld %s: %s";

    private final TypeTransformerRegistry transformers;
    private final JsonLd jsonLd;

    public Codec(TypeTransformerRegistry transformers, JsonLd jsonLd) {
        this.transformers = transformers;
        this.jsonLd = jsonLd;
    }

    public <T extends Entity> Result<List<T>> deserializeList(String entitiesJson, Class<T> type) {
        var assetsJsonArray = Json.createReader(new StringReader(entitiesJson)).readArray();

        var deserializedAssets = assetsJsonArray.stream()
                .map(asset -> deserialize(asset.toString(), type))
                .toList();

        // If any of the deserializations failed, fail the whole operation
        if (deserializedAssets.stream().anyMatch(Result::failed)) {
            List<String> errorMessages = deserializedAssets
                    .stream()
                    .filter(Result::failed)
                    .map(Result::getFailureDetail)
                    .toList();

            return Result.failure(errorMessages);
        }
        return Result.success(deserializedAssets.stream().map(Result::getContent).toList());
    }

    public <T extends Entity> Result<T> deserialize(String entityJson, Class<T> type) {
        var assetJsonObject = Json.createReader(new StringReader(entityJson)).readObject();

        var expandedResult = jsonLd.expand(assetJsonObject);

        if (expandedResult.failed()) {
            Failure failure = expandedResult.getFailure();
            return Result.failure(String.format(EXPANSION_ERROR, failure.getClass().getSimpleName(),
                    failure.getFailureDetail()));
        }

        return transformers.transform(expandedResult.getContent(), type);
    }

    public PolicyDefinition deserializePolicyDefinition(String policyDefinitionJson) {
        var policyDefinitionJsonObject = Json.createReader(new StringReader(policyDefinitionJson)).readObject();

        var expansionResult =
                jsonLd.expand(policyDefinitionJsonObject).orElseThrow(failure -> new EdcException(String.format(EXPANSION_ERROR,
                        failure.getClass().getSimpleName(), failure.getFailureDetail())));

        var policyDefinition = transformers.transform(expansionResult, PolicyDefinition.class)
                .orElseThrow(failure -> new EdcException(String.format(DESERIALIZATION_ERROR, PolicyDefinition.class.getSimpleName(),
                        failure.getFailureDetail())));

        // Transformer assigns random UUID to the asset. We don't need that.
        var policy = policyDefinition.getPolicy().toBuilder().build();

        return PolicyDefinition.Builder.newInstance()
                .privateProperties(policyDefinition.getPrivateProperties())
                .policy(policy).id(policyDefinition.getId())
                .build();
    }

    public String serialize(@NotNull Object toSerialize) {
        var jsonRepresentation =
                transformers.transform(toSerialize, JsonObject.class).orElseThrow(failure -> new EdcException(String.format(SERIALIZATION_ERROR,
                        toSerialize.getClass().getSimpleName(), failure.getFailureDetail())));

        var compacted = jsonLd.compact(jsonRepresentation).orElseThrow(failure -> new EdcException(String.format(COMPACTION_ERROR,
                toSerialize.getClass().getSimpleName(), failure.getFailureDetail())));
        return jsonRepresentation.toString();
    }
}
