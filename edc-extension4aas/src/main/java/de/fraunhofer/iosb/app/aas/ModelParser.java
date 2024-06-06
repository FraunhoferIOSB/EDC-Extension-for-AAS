package de.fraunhofer.iosb.app.aas;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.app.model.aas.CustomAssetAdministrationShell;
import de.fraunhofer.iosb.app.model.aas.CustomConceptDescription;
import de.fraunhofer.iosb.app.model.aas.CustomSubmodel;
import de.fraunhofer.iosb.app.model.aas.CustomSubmodelElement;
import de.fraunhofer.iosb.app.model.aas.CustomSubmodelElementCollection;
import de.fraunhofer.iosb.app.model.aas.CustomSubmodelElementList;
import de.fraunhofer.iosb.app.util.AssetAdministrationShellUtil;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.ConceptDescription;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultKey;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.eclipse.edc.spi.EdcException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;


/**
 * With string input this parser creates AAS model elements such as:
 * CustomAssetAdministrationShell, CustomConceptDescription, CustomSubmodel (with SubmodelElements)
 * In these
 */
public class ModelParser {

    private final JsonDeserializer jsonDeserializer = new JsonDeserializer();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String accessUrl;

    /**
     * Constructor
     *
     * @param url URL used for accessing the AAS service.
     */
    ModelParser(String url) {
        this.accessUrl = url;
    }

    List<CustomConceptDescription> parseConceptDescriptions(String conceptDescriptions) {
        var elementList = readList(conceptDescriptions, ConceptDescription.class);

        return elementList.stream()
                .map(CustomConceptDescription::fromConceptDescription)
                .peek(element -> {
                    element.setSourceUrl(accessUrl);
                    element.setReferenceChain(
                            createReference(KeyTypes.CONCEPT_DESCRIPTION, element.getId()));
                }).toList();
    }

    List<CustomAssetAdministrationShell> parseShells(String shells) {
        var elementList = readList(shells, AssetAdministrationShell.class);

        return elementList.stream()
                .map(CustomAssetAdministrationShell::fromAssetAdministrationShell)
                .peek(element -> {
                    element.setSourceUrl(accessUrl);
                    element.setReferenceChain(
                            createReference(KeyTypes.ASSET_ADMINISTRATION_SHELL, element.getId()));
                }).toList();
    }

    List<CustomSubmodel> parseSubmodels(String submodels, boolean onlySubmodels) {
        var elementList = readList(submodels, Submodel.class);

        // Map from real Submodel to CustomSubmodel, get SubmodelElements
        List<CustomSubmodel> customSubmodels = new ArrayList<>();

        for (Submodel submodel : elementList) {
            var customSubmodel = new CustomSubmodel();
            customSubmodel.setId(submodel.getId());
            customSubmodel.setIdShort(submodel.getIdShort());
            if (Objects.nonNull(submodel.getSemanticId())) {
                customSubmodel.setSemanticId(submodel.getSemanticId());
            }

            if (!onlySubmodels) {
                // Recursively add submodelElements
                var customElements = AssetAdministrationShellUtil.getCustomSubmodelElementStructureFromSubmodel(submodel);
                customSubmodel.setSubmodelElements((List<CustomSubmodelElement>) customElements);
            }
            customSubmodels.add(customSubmodel);
        }

        return customSubmodels.stream()
                .peek(element -> {
                    element.setSourceUrl(accessUrl);
                    element.setReferenceChain(
                            createReference(KeyTypes.SUBMODEL, element.getId()));
                    element.getSubmodelElements()
                            .forEach(elem -> putUrl(accessUrl, elem.getReferenceChain(), elem));
                }).toList();
    }


    /*
     * Add the access url + refChain of this element to its sourceUrl field. If this element
     * is a collection/list, do this recursively for all elements inside this collection/list,
     * too (since we don't know how deeply nested the collection is).
     */
    private void putUrl(String url, Reference parentReferenceChain, CustomSubmodelElement element) {
        Reference ownReferenceChain;

        // "value" field of a submodel element can be an array or not available (no collection)
        if (element instanceof CustomSubmodelElementCollection) {
            ownReferenceChain = createReference(KeyTypes.SUBMODEL_ELEMENT_COLLECTION, element.getIdShort(), parentReferenceChain);

            ((CustomSubmodelElementCollection) element).getValue().forEach(item -> putUrl(url, ownReferenceChain, item));

        } else if (element instanceof CustomSubmodelElementList) {
            ownReferenceChain = createReference(KeyTypes.SUBMODEL_ELEMENT_LIST, element.getIdShort(), parentReferenceChain);

            ((CustomSubmodelElementList) element).getValue().forEach(item -> putUrl(url, ownReferenceChain, item));

        } else { // Can not have any child elements...
            ownReferenceChain = createReference(KeyTypes.SUBMODEL_ELEMENT, element.getIdShort(), parentReferenceChain);
        }

        element.setSourceUrl(url);
        element.setReferenceChain(ownReferenceChain);
    }

    private <T> List<T> readList(String serialized, Class<T> clazz) {
        try {
            var responseJson = objectMapper.readTree(serialized).get("result");
            return jsonDeserializer.readList(responseJson, clazz);
        } catch (JsonProcessingException | DeserializationException e) {
            throw new EdcException(format("Failed parsing list of %s from %s", clazz.getName(), accessUrl), e);
        }
    }

    private Reference createReference(KeyTypes type, String value) {
        return new DefaultReference.Builder()
                .keys(new DefaultKey.Builder()
                        .type(type)
                        .value(value)
                        .build())
                .build();
    }

    private Reference createReference(KeyTypes type, String value, Reference parent) {

        return new DefaultReference.Builder()
                .keys(new ArrayList<>(parent.getKeys()))
                .keys(new DefaultKey.Builder()
                        .type(type)
                        .value(value)
                        .build())
                .build();
    }
}
