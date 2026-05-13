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
package de.fraunhofer.iosb.codec;

import jakarta.json.Json;
import org.eclipse.edc.connector.controlplane.transform.edc.contractdefinition.from.JsonObjectFromContractDefinitionTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.contractdefinition.to.JsonObjectToContractDefinitionTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.from.JsonObjectFromAssetTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.policy.from.JsonObjectFromPolicyDefinitionTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.policy.to.JsonObjectToPolicyDefinitionTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.to.JsonObjectToAssetTransformer;
import org.eclipse.edc.connector.controlplane.transform.odrl.OdrlTransformersFactory;
import org.eclipse.edc.connector.controlplane.transform.odrl.from.JsonObjectFromPolicyTransformer;
import org.eclipse.edc.connector.controlplane.transform.odrl.to.JsonObjectToActionTransformer;
import org.eclipse.edc.connector.controlplane.transform.odrl.to.JsonObjectToPolicyTransformer;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.participant.spi.ParticipantIdMapper;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromCriterionTransformer;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromDataAddressTransformer;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromQuerySpecTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToCriterionTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToDataAddressTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToQuerySpecTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonValueToGenericTypeTransformer;

import java.util.Map;
import java.util.Objects;

import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;


/**
 * EDC Extension supporting usage of Asset Administration Shells.
 */
@Extension(value = CodecExtension.NAME)
@Provides(Codec.class)
public class CodecExtension implements ServiceExtension {

    public static final String NAME = "Codec Extension";

    @Inject(required = false)
    private CriterionOperatorRegistry criterionOperatorRegistry;
    @Inject
    private JsonLd jsonLd;
    @Inject
    private TypeTransformerRegistry typeTransformerRegistry;
    @Inject
    private ParticipantIdMapper participantIdMapper;
    @Inject
    private TypeManager typeManager;


    @Override
    public void initialize(ServiceExtensionContext context) {
        registerTransformers();
    }


    @Provider(isDefault = true)
    Codec provideCodec() {
        return new Codec(typeTransformerRegistry, jsonLd);
    }


    private void registerTransformers() {
        var jsonFactory = Json.createBuilderFactory(Map.of());
        // Register (de)serializers
        OdrlTransformersFactory.jsonObjectToOdrlTransformers(participantIdMapper)
                .forEach(typeTransformerRegistry::register);
        typeTransformerRegistry.register(new JsonObjectFromCriterionTransformer(jsonFactory, typeManager, JSON_LD));
        typeTransformerRegistry.register(new JsonObjectFromPolicyTransformer(jsonFactory, participantIdMapper));
        typeTransformerRegistry.register(new JsonObjectFromPolicyDefinitionTransformer(jsonFactory, typeManager, JSON_LD));
        typeTransformerRegistry.register(new JsonObjectFromContractDefinitionTransformer(jsonFactory, typeManager, JSON_LD));
        typeTransformerRegistry.register(new JsonObjectFromQuerySpecTransformer(jsonFactory));
        typeTransformerRegistry.register(new JsonObjectFromAssetTransformer(jsonFactory, typeManager, JSON_LD));
        typeTransformerRegistry.register(new JsonObjectFromDataAddressTransformer(jsonFactory, typeManager, JSON_LD));
        typeTransformerRegistry.register(new JsonObjectToActionTransformer());
        var operatorRegistry = Objects.requireNonNullElse(criterionOperatorRegistry, new CriterionOperatorRegistryImpl());
        typeTransformerRegistry.register(new JsonObjectToCriterionTransformer(operatorRegistry));
        typeTransformerRegistry.register(new JsonObjectToPolicyTransformer(participantIdMapper));
        typeTransformerRegistry.register(new JsonObjectToPolicyDefinitionTransformer());
        typeTransformerRegistry.register(new JsonObjectToContractDefinitionTransformer());
        typeTransformerRegistry.register(new JsonObjectToQuerySpecTransformer());
        typeTransformerRegistry.register(new JsonObjectToAssetTransformer());
        typeTransformerRegistry.register(new JsonObjectToDataAddressTransformer());
        typeTransformerRegistry.register(new JsonValueToGenericTypeTransformer(typeManager, JSON_LD));
    }
}
