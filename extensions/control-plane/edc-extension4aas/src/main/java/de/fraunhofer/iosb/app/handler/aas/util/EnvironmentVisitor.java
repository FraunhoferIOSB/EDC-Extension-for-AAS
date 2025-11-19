package de.fraunhofer.iosb.app.handler.aas.util;

import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.util.AasUtils;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Identifiable;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;


public record EnvironmentVisitor(Environment environment) {

    public EnvironmentVisitor visitShells(Function<Identifiable, Identifiable> visitor) {
        environment.getAssetAdministrationShells().forEach(visitor::apply);
        return this;
    }


    public EnvironmentVisitor visitShells(Predicate<Identifiable> eliminator) {
        environment.setAssetAdministrationShells(environment.getAssetAdministrationShells().stream().filter(eliminator).toList());
        return this;
    }


    public EnvironmentVisitor visitConceptDescriptions(Function<Identifiable, Identifiable> visitor) {
        environment.getConceptDescriptions().forEach(visitor::apply);
        return this;
    }


    public EnvironmentVisitor visitConceptDescriptions(Predicate<Identifiable> eliminator) {
        environment.setConceptDescriptions(environment.getConceptDescriptions().stream().filter(eliminator).toList());
        return this;
    }


    public EnvironmentVisitor visitSubmodels(Function<Identifiable, Identifiable> visitor,
                                             BiFunction<Reference, SubmodelElement, SubmodelElement> childVisitor) {
        environment.getSubmodels().forEach(submodel ->
                submodel.getSubmodelElements().forEach(submodelElement -> childVisitor.apply(AasUtils.toReference(submodel),
                        submodelElement)));
        environment.getSubmodels().forEach(visitor::apply);
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
