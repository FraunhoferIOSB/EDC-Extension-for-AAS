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
package de.fraunhofer.iosb.app.pipeline.helper;

import de.fraunhofer.iosb.app.pipeline.PipelineResult;
import de.fraunhofer.iosb.app.pipeline.PipelineStep;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static de.fraunhofer.iosb.app.pipeline.util.PipelineUtils.handleError;

/**
 * Process the values of the input map only. The result is a map with following structure: (input_key, processed_value)
 *
 * @param <T> Input and output map key type, not to be processed.
 * @param <U> Input map value type, to be processed by the processor.
 * @param <V> Output map value type, result type of the processor.
 */
public class MapValueProcessor<T, U, V> extends PipelineStep<Map<T, U>, Map<T, V>> {

    private final PipelineStep<U, V> processor;
    private final Function<PipelineResult<V>, PipelineResult<V>> additionalFailureHandling;

    /**
     * Initializes a map value processor with default failure handling.
     *
     * @param processor Process each value from the input map.
     */
    public MapValueProcessor(PipelineStep<U, V> processor) {
        this.processor = processor;
        this.additionalFailureHandling = Function.identity();
    }

    /**
     * Class constructor with additional failure handling possibility.
     * The failureHandling function is called before it is known that a PipelineResult has failed.
     *
     * @param processor                 Process each value from the input map.
     * @param additionalFailureHandling Handle failures explicitly.
     */
    public MapValueProcessor(PipelineStep<U, V> processor,
                             Function<PipelineResult<V>, PipelineResult<V>> additionalFailureHandling) {
        this.processor = processor;
        this.additionalFailureHandling = additionalFailureHandling;
    }

    @Override
    public PipelineResult<Map<T, V>> apply(Map<T, U> tuMap) {
        List<PipelineResult<V>> results = new LinkedList<>();
        var contents = new HashMap<T, V>();
        tuMap.forEach((t, u) -> {
            var processingResult = processor.apply(u);
            if (processingResult.failed()) {
                processingResult = additionalFailureHandling.apply(processingResult);
            }
            results.add(processingResult);
            if (processingResult != null && processingResult.getContent() != null) {
                contents.put(t, processingResult.getContent());
            }
        });

        return Objects.requireNonNullElseGet(handleError(results, contents), () -> PipelineResult.success(contents));
    }

    @Override
    public String getName() {
        return "MapValueProcessor[%s]".formatted(processor.getName());
    }
}
