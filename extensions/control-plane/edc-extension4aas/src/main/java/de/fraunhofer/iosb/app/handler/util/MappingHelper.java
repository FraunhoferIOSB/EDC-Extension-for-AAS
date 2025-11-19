package de.fraunhofer.iosb.app.handler.util;

import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.util.AasUtils;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Identifiable;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementCollection;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;


/**
 *
 */
public abstract class MappingHelper {

    public static <T> Map<Reference, T> map(Environment environment, Function<Identifiable, T> identifiableMapper,
                                            BiFunction<Reference, SubmodelElement, T> elementMapper) {
        Map<Reference, T> result = new HashMap<>();

        environment.getAssetAdministrationShells().stream()
                .map(shell -> mapIdentifiable(shell, identifiableMapper, elementMapper))
                .forEach(result::putAll);

        environment.getConceptDescriptions().stream()
                .map(conceptDescription -> mapIdentifiable(conceptDescription, identifiableMapper, elementMapper))
                .forEach(result::putAll);

        environment.getSubmodels().stream()
                .map(submodel -> mapIdentifiable(submodel, identifiableMapper, elementMapper))
                .forEach(result::putAll);

        return result;
    }


    private static <T> Map<Reference, T> mapIdentifiable(Identifiable identifiable,
                                                         Function<Identifiable, T> mappingFunction,
                                                         BiFunction<Reference, SubmodelElement, T> elementMappingFunction) {
        Map<Reference, T> filteredElements = new HashMap<>();

        Reference identifiableReference = AasUtils.toReference(identifiable);

        if (identifiable instanceof Submodel submodel) {
            submodel.getSubmodelElements().stream()
                    .map(element -> mapSubmodelElement(identifiableReference, element, elementMappingFunction))
                    .forEach(filteredElements::putAll);
        }

        filteredElements.put(identifiableReference, mappingFunction.apply(identifiable));

        return filteredElements;
    }


    private static <T> Map<Reference, T> mapSubmodelElement(Reference parent, SubmodelElement submodelElement,
                                                            BiFunction<Reference, SubmodelElement, T> mappingFunction) {
        Map<Reference, T> result = new HashMap<>();

        Reference submodelElementReference = AasUtils.toReference(parent, submodelElement);

        if (submodelElement instanceof SubmodelElementList list) {
            List<SubmodelElement> children = list.getValue();
            // AASd-120 - aware
            for (int i = 0; i < children.size(); i++) {
                SubmodelElement child = children.get(i);
                child.setIdShort(String.valueOf(i));
                result.putAll(mapSubmodelElement(submodelElementReference, child, mappingFunction));
            }
        }
        else if (submodelElement instanceof SubmodelElementCollection collection) {
            collection.getValue().stream()
                    .map(child -> mapSubmodelElement(submodelElementReference, child, mappingFunction))
                    .forEach(result::putAll);
        }

        result.put(submodelElementReference, mappingFunction.apply(parent, submodelElement));

        return result;
    }
}
