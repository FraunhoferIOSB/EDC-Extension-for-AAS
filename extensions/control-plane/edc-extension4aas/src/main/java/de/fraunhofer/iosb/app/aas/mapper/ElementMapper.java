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
package de.fraunhofer.iosb.app.aas.mapper;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import de.fraunhofer.iosb.client.AasServerClient;
import de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.annotations.IRI;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;


/**
 * Contains base logic for mapping AAS elements to Assets
 */
public class ElementMapper {

    private final ObjectMapper objectMapper;
    private final TypeReference<Map<String, Object>> jsonMapTypeRef = new TypeReference<>() {
    };
    private final TypeReference<List<Object>> jsonListTypeRef = new TypeReference<>() {
    };
    private final AasServerClient client;


    protected ElementMapper(AasServerClient client) {
        this.client = client;
        objectMapper = new ObjectMapper()
                .setAnnotationIntrospector(new NamespacingIntrospector())
                // Disable auto-detection from method names
                .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                // Enable auto-detection by field names (where @IRI is placed)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }


    @NotNull
    public String generateId(Reference reference) {
        var aasDataAddress = createDataAddress(reference);
        return String.valueOf("%s:%s".formatted(aasDataAddress.getBaseUrl(), aasDataAddress.getPath()).hashCode());
    }


    protected AasDataAddress createDataAddress(Reference reference) {
        AasDataAddress.Builder builder = AasDataAddress.Builder.newInstance()
                .baseUrl(client.getUri().toString())
                .reference(reference);

        if (client.requiresAuthentication()) {
            builder.additionalHeaders(client.getHeaders());
        }

        return builder.build();
    }


    protected Map<String, Object> getNamespaced(Object object) {
        return objectMapper.convertValue(object, jsonMapTypeRef);
    }


    protected List<Object> getNamespacedList(Object object) {
        return objectMapper.convertValue(object, jsonListTypeRef);
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
