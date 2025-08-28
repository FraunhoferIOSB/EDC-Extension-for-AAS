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

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.entity.Entity;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.io.StringReader;
import java.util.List;

public class Codec {

    private static final String SERIALIZATION_ERROR = "Failed serializing %s: %s";
    private static final String DESERIALIZATION_ERROR = "Failed deserializing %s: %s";
    private static final String COMPACTION_ERROR = "Failed compacting json-ld %s: %s";
    private static final String EXPANSION_ERROR = "Failed expanding json-ld %s: %s";

    protected final TypeTransformerRegistry transformers;
    protected final TypeManager typeManager;
    protected final JsonLd jsonLd;

    public Codec(TypeTransformerRegistry transformers, TypeManager typeManager, JsonLd jsonLd) {
        this.transformers = transformers;
        this.typeManager = typeManager;
        this.jsonLd = jsonLd;
    }

    public List<Asset> deserializeAssets(String assetJsonString) {
        var assetsJsonArray = Json.createReader(new StringReader(assetJsonString)).readArray();
        return assetsJsonArray.stream().map(asset -> deserializeAsset(asset.toString())).toList();
    }

    public Asset deserializeAsset(String assetJsonString) {
        try {
            var deserialized = typeManager.getMapper().readValue(assetJsonString, Asset.class);
            // TypeManager assigns new random UUID to the asset. We need the original one though
            deserialized = deserialized.toBuilder().id(deserialized.getProperty("id").toString()).build();
            return deserialized;
        } catch (JsonProcessingException assetDeserializationException) {
            throw new EdcException(String.format(DESERIALIZATION_ERROR, Asset.class.getSimpleName(), assetDeserializationException.getMessage()),
                    assetDeserializationException);
        }
    }

    public String serialize(Entity toSerialize) {
        var jsonRepresentation =
                transformers.transform(toSerialize, JsonObject.class).orElseThrow(failure -> new EdcException(String.format(SERIALIZATION_ERROR,
                        toSerialize.getClass().getSimpleName(), failure.getFailureDetail())));

        var compacted = jsonLd.compact(jsonRepresentation).orElseThrow(failure -> new EdcException(String.format(COMPACTION_ERROR,
                toSerialize.getClass().getSimpleName(), failure.getFailureDetail())));

        return compacted.toString();
    }

    public List<PolicyDefinition> deserializePolicyDefinitions(String policyDefinitionJson) {
        var policyDefinitionsJsonArray = Json.createReader(new StringReader(policyDefinitionJson)).readArray();
        return policyDefinitionsJsonArray.stream().map(def -> deserializePolicyDefinition(def.toString())).toList();
    }

    public PolicyDefinition deserializePolicyDefinition(String policyDefinitionJson) {
        var policyDefinitionJsonObject = Json.createReader(new StringReader(policyDefinitionJson)).readObject();

        var expansionResult =
                jsonLd.expand(policyDefinitionJsonObject).orElseThrow(failure -> new EdcException(String.format(EXPANSION_ERROR,
                        failure.getClass().getSimpleName(), failure.getFailureDetail())));

        return transformers.transform(expansionResult, PolicyDefinition.class).orElseThrow(failure -> new EdcException(String.format(DESERIALIZATION_ERROR, failure.getClass().getSimpleName(), failure.getFailureDetail())));
    }

    public String serializeQuerySpec(QuerySpec spec) {
        var querySpecSerializationResult =
                transformers.transform(spec, JsonObject.class).orElseThrow(failure -> new EdcException(String.format(SERIALIZATION_ERROR
                        , spec.getClass().getSimpleName(), failure.getFailureDetail())));

        return querySpecSerializationResult.toString();
    }

    public List<ContractDefinition> deserializeContractDefinitions(String contractDefinitionsJsonString) {
        var contractDefinitionsJsonArray = Json.createReader(new StringReader(contractDefinitionsJsonString)).readArray();
        return contractDefinitionsJsonArray.stream().map(contract -> deserializeContractDefinition(contract.toString())).toList();
    }

    public ContractDefinition deserializeContractDefinition(String contractDefinitionJsonString) {
        var contractDefinitionJsonObject = Json.createReader(new StringReader(contractDefinitionJsonString)).readObject();

        var expansionResult =
                jsonLd.expand(contractDefinitionJsonObject).orElseThrow(failure -> new EdcException(String.format(EXPANSION_ERROR,
                        failure.getClass().getSimpleName(), failure.getFailureDetail())));

        return transformers.transform(expansionResult, ContractDefinition.class).orElseThrow(failure -> new EdcException(String.format(DESERIALIZATION_ERROR,
                failure.getClass().getSimpleName(), failure.getFailureDetail())));
    }

}
