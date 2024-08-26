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
package de.fraunhofer.iosb.app.testutils;

import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetKind;
import org.eclipse.digitaltwin.aas4j.v3.model.ConceptDescription;
import org.eclipse.digitaltwin.aas4j.v3.model.DataTypeDefXsd;
import org.eclipse.digitaltwin.aas4j.v3.model.DataTypeIec61360;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.ModellingKind;
import org.eclipse.digitaltwin.aas4j.v3.model.Property;
import org.eclipse.digitaltwin.aas4j.v3.model.ReferenceTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementCollection;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAdministrativeInformation;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetInformation;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultConceptDescription;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultDataSpecificationIec61360;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEmbeddedDataSpecification;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEnvironment;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultExtension;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultKey;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultLangStringNameType;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultLangStringTextType;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultProperty;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSpecificAssetId;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodelElementCollection;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Create AAS elements with default values.
 */
public class AasCreator {

    private AasCreator() {
        throw new RuntimeException("Utility class");
    }

    public static Environment getEmptyEnvironment() {
        return new DefaultEnvironment.Builder().build();
    }

    public static Environment getEnvironment() {
        return new DefaultEnvironment.Builder()
                .submodels(List.of(getSubmodel(), getSubmodel(), getSubmodel()))
                .assetAdministrationShells(List.of(getAssetAdministrationShell(), getAssetAdministrationShell(), getAssetAdministrationShell()))
                .conceptDescriptions(List.of(getConceptDescription(), getConceptDescription(), getConceptDescription()))
                .build();
    }

    public static ConceptDescription getConceptDescription() {
        return getConceptDescription(uuid());
    }

    public static ConceptDescription getConceptDescription(String id) {
        return new DefaultConceptDescription.Builder()
                .embeddedDataSpecifications(getEmbeddedDataSpecifications(id))
                .extensions(getExtensions(id))
                .administration(getAdministrativeInformation(id))
                .id(id)
                .idShort(id)
                .category(id)
                .description(getLangStringTextType(id))
                .displayName(getLangStringNameType())
                .build();
    }

    static DefaultExtension getExtensions(String id) {
        return new DefaultExtension.Builder()
                .name(id)
                .value(id)
                .valueType(DataTypeDefXsd.ANY_URI)
                .build();
    }

    public static AssetAdministrationShell getAssetAdministrationShell() {
        return getAssetAdministrationShell(uuid());
    }

    public static AssetAdministrationShell getAssetAdministrationShell(String id) {
        return new DefaultAssetAdministrationShell.Builder()
                .assetInformation(getAssetInformation(id))
                .derivedFrom(getReference(id))
                .submodels(List.of(getSubmodel().getSemanticId(),
                        getSubmodel().getSemanticId(),
                        getSubmodel().getSemanticId()))
                .embeddedDataSpecifications(getEmbeddedDataSpecifications(id))
                .extensions(getExtensions(id))
                .administration(getAdministrativeInformation(id))
                .id(id)
                .category(id)
                .description(getLangStringTextType("aas"))
                .displayName(getLangStringNameType())
                .idShort(id)
                .build();
    }

    private static DefaultAssetInformation getAssetInformation(String id) {
        return new DefaultAssetInformation.Builder()
                .assetKind(AssetKind.INSTANCE)
                .globalAssetId(id)
                .specificAssetIds(new DefaultSpecificAssetId.Builder()
                        .value(id)
                        .name("AAS specific asset id")
                        .build())
                .build();
    }

    public static Submodel getEmptySubmodel() {
        return new DefaultSubmodel.Builder()
                .id(uuid())
                .idShort(uuid()).build();
    }

    public static Submodel getSubmodel() {
        return getSubmodel(uuid());
    }

    public static Submodel getSubmodel(String id) {
        return new DefaultSubmodel.Builder()
                .idShort(id)
                .id(id)
                .kind(ModellingKind.INSTANCE)
                .submodelElements(List.of(getProperty(), getProperty(),
                        getProperty(), getSubmodelElementCollection(4)))
                .category("Submodel category")
                .semanticId(getReference(id))
                .embeddedDataSpecifications(getEmbeddedDataSpecifications(id))
                .description(getLangStringTextType(id))
                .build();
    }

    public static SubmodelElementCollection getSubmodelElementCollection(int level) {
        return getSubmodelElementCollection(level, uuid());
    }

    public static SubmodelElementCollection getSubmodelElementCollection(int level, String id) {
        var smcBuilder = new DefaultSubmodelElementCollection.Builder()
                .idShort(id)
                .value(new ArrayList<>(List.of(getProperty(), getProperty())))
                .category("SubmodelElementCollection category")
                .embeddedDataSpecifications(getEmbeddedDataSpecifications(id))
                .description(new DefaultLangStringTextType.Builder().text("SubmodelElementCollection description").language("en").build());

        if (level < 1) {
            return smcBuilder.build();
        }
        return smcBuilder.value(getSubmodelElementCollection(level - 1, id + "::" + id)).build();
    }

    public static Property getProperty() {
        return getProperty(uuid());
    }

    public static Property getProperty(String id) {
        return new DefaultProperty.Builder()
                .idShort(id)
                .category("Property category")
                .embeddedDataSpecifications(getEmbeddedDataSpecifications(id))
                .value("Property value")
                .description(new DefaultLangStringTextType.Builder().text("Property description").language("en").build())
                .build();
    }

    static DefaultReference getReference(String id) {
        return new DefaultReference.Builder()
                .type(ReferenceTypes.EXTERNAL_REFERENCE)
                .keys(new DefaultKey.Builder()
                        .type(KeyTypes.CONCEPT_DESCRIPTION)
                        .value(id)
                        .build())
                .build();
    }

    static DefaultLangStringNameType getLangStringNameType() {
        return new DefaultLangStringNameType.Builder()
                .language("en")
                .text("Display Name AAS")
                .build();
    }

    static DefaultLangStringTextType getLangStringTextType(String type) {
        return new DefaultLangStringTextType.Builder()
                .language("en")
                .text("Description %s".formatted(type))
                .build();
    }

    static DefaultAdministrativeInformation getAdministrativeInformation(String id) {
        return new DefaultAdministrativeInformation.Builder()
                .embeddedDataSpecifications(getEmbeddedDataSpecifications(id))
                .build();
    }

    static DefaultEmbeddedDataSpecification getEmbeddedDataSpecifications(String id) {
        return new DefaultEmbeddedDataSpecification.Builder()
                .dataSpecificationContent(new DefaultDataSpecificationIec61360.Builder()
                        .dataType(DataTypeIec61360.STRING)
                        .value(id)
                        .build()).build();
    }

    static String uuid() {
        return UUID.randomUUID().toString();
    }
}
