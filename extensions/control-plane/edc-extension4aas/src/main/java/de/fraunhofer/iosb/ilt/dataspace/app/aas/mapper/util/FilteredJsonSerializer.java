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
package de.fraunhofer.iosb.ilt.dataspace.app.aas.mapper.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonSerializer;
import org.eclipse.digitaltwin.aas4j.v3.model.annotations.IRI;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.PropertyName;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonInclude.Value.construct;
import static de.fraunhofer.iosb.ilt.dataspace.constants.AasConstants.AAS_V31_NAMESPACE;


/**
 * Extension of the aas4j-JsonSerializer that is able to remove unwanted fields from AAS objects.
 */
public class FilteredJsonSerializer extends JsonSerializer {
    private static final TypeReference<Map<String, Object>> JSON_MAP_TYPE_REF = new TypeReference<>() {};

    /**
     * Constructor of class
     */
    public FilteredJsonSerializer() {
        super();
        Class<?> type = JsonSerializer.class;

        System.out.println("Loaded from: "
                + type.getProtectionDomain().getCodeSource().getLocation());

        for (var field: type.getDeclaredFields()) {
            System.out.println(field.getName() + ": " + field.getType().getName());
        }

        mapper = mapper.rebuild()
                // drop empty lists/maps/arrays,empty strings, null elements
                .changeDefaultPropertyInclusion(inclusion -> construct(JsonInclude.Include.NON_EMPTY, JsonInclude.Include.NON_NULL))
                .build();
    }


    /**
     * Extends the default write() method with a filter to avoid unwanted fields from being exposed via self-description.
     *
     * @param aasInstance the AAS instance to serialize
     * @param allowedFields Fields of the AAS object to keep during serialization. Fields not mentioned here will be removed
     * @return the string representation
     * @throws SerializationException if serialization fails
     */
    public String write(Object aasInstance, Set<String> allowedFields) throws SerializationException {
        // Store copy of unmodified mapper
        var mapperRebuilder = mapper.rebuild();
        try {
            mapperRebuilder.addModule(moduleFor(allowedFields));
            return super.write(aasInstance);
        }
        finally {
            // Be sure to not keep modified mapper
            mapper = mapperRebuilder.build();
        }
    }


    /**
     * Extends the default write() method with a filter to avoid unwanted fields from being exposed via catalog.
     *
     * @param aasInstance the AAS instance to serialize
     * @param allowedFields Fields of the AAS object to keep during serialization. Fields not mentioned here will be
     *            removed.
     * @return the filtered map representation
     */
    public Map<String, Object> toMap(Object aasInstance, Set<String> allowedFields) {
        return mapper.rebuild()
                .annotationIntrospector(new NamespacingIntrospector())
                .addModule(moduleFor(allowedFields))
                .build()
                .convertValue(aasInstance, JSON_MAP_TYPE_REF);
    }


    private SimpleModule moduleFor(Set<String> allowedFields) {
        Set<String> allowedIris = allowedFields.stream()
                .map(AAS_V31_NAMESPACE::concat)
                .collect(java.util.stream.Collectors.toSet());

        SimpleModule module = new SimpleModule();
        // Ignore fields that should not be exposed in the self-description / catalog
        module.setSerializerModifier(new ValueSerializerModifier() {
            @Override
            public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription.Supplier beanDesc, List<BeanPropertyWriter> beanProperties) {
                return beanProperties.stream()
                        .filter(w -> {
                            String iri = getIriValue(w);
                            // Keep property if no IRI or IRI is not in the ignored set
                            return iri != null && allowedIris.contains(iri);
                        })
                        .toList();
            }


            private String getIriValue(BeanPropertyWriter w) {
                AnnotatedMember m = w.getMember();
                if (m == null) {
                    return null;
                }
                IRI ann = m.getAnnotation(IRI.class);
                return ann != null && ann.value().length > 0 ? ann.value()[0] : null;
            }
        });

        return module;
    }

    private static class NamespacingIntrospector extends JacksonAnnotationIntrospector {

        @Override
        public PropertyName findNameForSerialization(MapperConfig<?> config, Annotated a) {
            IRI iri = a.getAnnotation(IRI.class);
            if (iri != null && iri.value().length > 0) {
                return PropertyName.construct(iri.value()[0]);
            }
            return super.findNameForSerialization(config, a);
        }
    }
}
