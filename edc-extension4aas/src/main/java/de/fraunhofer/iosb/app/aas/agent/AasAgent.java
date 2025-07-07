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
import de.fraunhofer.iosb.aas.AasDataProcessor;
import de.fraunhofer.iosb.aas.AasDataProcessorFactory;
import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress;
import de.fraunhofer.iosb.model.aas.AasProvider;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static jakarta.ws.rs.HttpMethod.GET;

/**
 * Fetching an AAS environment from AAS service or AAS registry providers.
 */
public abstract class AasAgent<T extends AasProvider, U> extends PipelineStep<T, U> {

    protected final AasDataProcessorFactory aasDataProcessorFactory;
    private final JsonDeserializer jsonDeserializer = new JsonDeserializer();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AasAgent(AasDataProcessorFactory aasDataProcessorFactory) {
        this.aasDataProcessorFactory = aasDataProcessorFactory;
    }

    protected <K> Result<List<K>> readElements(AasDataProcessor processor, AasProvider provider, String path,
                                               Class<K> clazz) {
        var dataAddress = AasDataAddress.Builder.newInstance()
                .method(GET)
                .aasProvider(provider)
                .path(path)
                .build();
        var responseResult = executeRequest(processor, dataAddress);

        if (responseResult.failed()) {
            System.out.println(responseResult.getFailureMessages().toString());
            System.out.println(responseResult.getFailure().getMessages().toString());
            return Result.failure("Reading %s from %s failed: %s"
                    .formatted(clazz.getName(), path, responseResult.getFailureDetail()));
        }

        var response = responseResult.getContent();

        if (response.isSuccessful() && response.body() != null) {
            return readList(response.body(), clazz);
        }

        return Result.failure("Reading %s from %s failed: %s, %s"
                .formatted(clazz.getSimpleName(), path, response.code(), response.message()));
    }

    private Result<Response> executeRequest(AasDataProcessor processor, AasDataAddress dataAddress) {
        try {
            return Result.success(processor.send(dataAddress));
        } catch (IOException httpIOException) {
            return Result.failure(List.of(httpIOException.getClass().getSimpleName(), httpIOException.getMessage()));
        }
    }

    private <K> @Nonnull Result<List<K>> readList(@Nullable ResponseBody responseBody, Class<K> clazz) {
        String serialized;
        try {
            serialized = responseBody == null ? null : responseBody.string();
        } catch (IOException readBodyException) {
            return Result.failure("Failed reading response body: %s, %s"
                    .formatted(readBodyException.getClass().getSimpleName(), readBodyException.getMessage()));
        }

        try {
            var responseJson = objectMapper.readTree(serialized).get("result");
            return Result.success(Optional.ofNullable(jsonDeserializer.readList(responseJson, clazz))
                    .orElse(new ArrayList<>()));
        } catch (JsonProcessingException | DeserializationException e) {
            return Result.failure(List.of("Failed parsing list of %s".formatted(clazz.getName()), e.getMessage(), ExceptionUtils.getRootCauseMessage(e)));
        }
    }
}
