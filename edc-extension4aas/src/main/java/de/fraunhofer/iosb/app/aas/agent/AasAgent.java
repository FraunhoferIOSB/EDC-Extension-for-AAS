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
package de.fraunhofer.iosb.app.aas.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.aas.AasDataProcessorFactory;
import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.edc.spi.EdcException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository.SelfDescriptionSourceType;
import static jakarta.ws.rs.HttpMethod.GET;
import static java.lang.String.format;

/**
 * Fetching an AAS environment from AAS service or AAS registry providers.
 */
public abstract class AasAgent extends PipelineStep<URL, Map<URL, Environment>> {

    protected static final int INTERNAL_SERVER_ERROR = 500;

    private final AasDataProcessorFactory aasDataProcessorFactory;
    private final JsonDeserializer jsonDeserializer = new JsonDeserializer();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AasAgent(AasDataProcessorFactory aasDataProcessorFactory) {
        this.aasDataProcessorFactory = aasDataProcessorFactory;
    }

    public abstract SelfDescriptionSourceType supportedType();

    protected <T> List<T> readElements(URL accessUrl, Class<T> clazz) throws IOException {
        try (var response = executeRequest(accessUrl)) {
            if (response == null || response.body() == null || response.code() == INTERNAL_SERVER_ERROR) {
                throw new EdcException("Received empty body for %s request".formatted(clazz.getName()));
            }
            var body = response.body();

            return readList(body.string(), clazz);
        }
    }

    private <T> @NotNull List<T> readList(@Nullable String serialized, Class<T> clazz) {
        try {
            var responseJson = objectMapper.readTree(serialized).get("result");
            var xd = Optional.ofNullable(jsonDeserializer.readList(responseJson, clazz)).orElse(new ArrayList<>()); // TODO
            return Optional.ofNullable(jsonDeserializer.readList(responseJson, clazz))
                    .orElse(new ArrayList<>());
        } catch (JsonProcessingException | DeserializationException e) {
            throw new EdcException(format("Failed parsing list of %s", clazz.getName()), e);
        }
    }


    protected Response executeRequest(URL aasServiceUrl) throws IOException {
        var processor = aasDataProcessorFactory.processorFor(aasServiceUrl.toString());

        if (processor.failed()) {
            return new Response.Builder()
                    .code(500)
                    .body(ResponseBody.create(String.valueOf(processor.getFailure()), MediaType.get("application/json"
                    )))
                    .request(new Request.Builder().url("").build())
                    .build();
        }
        return processor.getContent()
                .send(AasDataAddress.Builder
                        .newInstance()
                        .method(GET)
                        .baseUrl(aasServiceUrl.toString())
                        .build());

    }
}
