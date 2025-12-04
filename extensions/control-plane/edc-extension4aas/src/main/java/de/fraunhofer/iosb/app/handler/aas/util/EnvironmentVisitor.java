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
package de.fraunhofer.iosb.app.handler.aas.util;

import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.util.AasUtils;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Identifiable;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;


/**
 * Visitor for environment shells, submodels, concept-descriptions, and submodel#submodelElements.
 * <p>
 * Allows Consumers to modify and Predicates to filter the environment's identifiable elements. Allows BiFunctions to alter and filter SubmodelElements. Note: BiFunctions should be
 * recursive if the elements within a SubmodelCollection/List shall be altered/filtered too.
 *
 * @param environment The environment to visit.
 */
public record EnvironmentVisitor(Environment environment) {

    public EnvironmentVisitor visitShells(Consumer<Identifiable> visitor) {
        environment.getAssetAdministrationShells().forEach(visitor);
        return this;
    }


    public EnvironmentVisitor visitShells(Predicate<Identifiable> eliminator) {
        environment.setAssetAdministrationShells(environment.getAssetAdministrationShells().stream().filter(eliminator).toList());
        return this;
    }


    public EnvironmentVisitor visitConceptDescriptions(Consumer<Identifiable> visitor) {
        environment.getConceptDescriptions().forEach(visitor);
        return this;
    }


    public EnvironmentVisitor visitConceptDescriptions(Predicate<Identifiable> eliminator) {
        environment.setConceptDescriptions(environment.getConceptDescriptions().stream().filter(eliminator).toList());
        return this;
    }


    public EnvironmentVisitor visitSubmodels(Consumer<Identifiable> visitor,
                                             BiFunction<Reference, SubmodelElement, SubmodelElement> childVisitor) {
        environment.getSubmodels().forEach(submodel ->
                submodel.getSubmodelElements().forEach(submodelElement -> childVisitor.apply(AasUtils.toReference(submodel),
                        submodelElement)));
        environment.getSubmodels().forEach(visitor);
        return this;
    }


    public EnvironmentVisitor visitSubmodels(Predicate<Identifiable> eliminator,
                                             BiFunction<Reference, SubmodelElement, SubmodelElement> childEliminator) {
        environment.getSubmodels().forEach(submodel ->
                submodel.setSubmodelElements(
                        submodel.getSubmodelElements().stream()
                                .map(submodelElement ->
                                        childEliminator.apply(AasUtils.toReference(submodel), submodelElement))
                                .filter(Objects::nonNull)
                                .toList()));

        environment.setSubmodels(environment.getSubmodels().stream().filter(eliminator).toList());
        return this;
    }
}
