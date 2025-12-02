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
package de.fraunhofer.iosb.app.aas.mapper.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonSerializer;
import org.eclipse.digitaltwin.aas4j.v3.model.annotations.IRI;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static de.fraunhofer.iosb.constants.AasConstants.AAS_V30_NAMESPACE;


/**
 * Extension of the aas4j-JsonSerializer that is able to remove unwanted fields from AAS objects.
 */
public class FilteredJsonSerializer extends JsonSerializer {
    private static final TypeReference<Map<String, Object>> JSON_MAP_TYPE_REF = new TypeReference<>() {
    };


    public FilteredJsonSerializer() {
        mapper.setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY,  // drop empty lists/maps/arrays and empty strings
                JsonInclude.Include.NON_NULL    // optional: drop null elements inside containers
        ));
    }


    /**
     * Extends the default write() method with a filter to avoid unwanted fields from being exposed via self-description.
     *
     * @param aasInstance the AAS instance to serialize
     * @return the string representation
     * @throws SerializationException if serialization fails
     */
    public String write(Object aasInstance, Set<String> allowedFields) throws SerializationException {
        // Store copy of unmodified mapper
        JsonMapper mapperCopy = mapper.copy();
        try {
            mapper.registerModule(moduleFor(allowedFields, true));
            return super.write(aasInstance);
        }
        finally {
            // Be sure to not keep modified mapper
            mapper = mapperCopy;
        }
    }


    /**
     * Extends the default writeList() method with a filter to avoid unwanted fields from being exposed via self-description.
     *
     * @param collection the collection to serialize. Not null.
     * @return the string representation of the collection.
     * @throws SerializationException if serialization fails
     */
    public String writeList(Collection<?> collection, Set<String> allowedFields) throws SerializationException {
        // Store copy of unmodified mapper
        JsonMapper mapperCopy = mapper.copy();
        try {
            mapper.registerModule(moduleFor(allowedFields, false));
            return super.writeList(collection);
        }
        finally {
            // Be sure to not keep modified mapper
            mapper = mapperCopy;
        }
    }


    /**
     * Extends the default write() method with a filter to avoid unwanted fields from being exposed via catalog.
     *
     * @param aasInstance the AAS instance to serialize
     * @return the filtered map representation
     */
    public Map<String, Object> toMap(Object aasInstance, Set<String> allowedFields) {
        return mapper.copy()
                .setAnnotationIntrospector(new NamespacingIntrospector())
                .registerModule(moduleFor(allowedFields, true))
                .convertValue(aasInstance, JSON_MAP_TYPE_REF);
    }


    private SimpleModule moduleFor(Set<String> allowedFields, boolean namespaced) {
        Set<String> allowedIris = namespaced ?
                allowedFields.stream()
                        .map(AAS_V30_NAMESPACE::concat)
                        .collect(java.util.stream.Collectors.toSet()) :
                allowedFields;

        SimpleModule module = new SimpleModule();
        // Ignore fields that should not be exposed in the self-description / catalog
        module.setSerializerModifier(new BeanSerializerModifier() {
            @Override
            public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
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
        public PropertyName findNameForSerialization(Annotated a) {
            IRI iri = a.getAnnotation(IRI.class);
            if (iri != null && iri.value().length > 0) {
                return PropertyName.construct(iri.value()[0]);
            }
            return super.findNameForSerialization(a);
        }
    }
}
