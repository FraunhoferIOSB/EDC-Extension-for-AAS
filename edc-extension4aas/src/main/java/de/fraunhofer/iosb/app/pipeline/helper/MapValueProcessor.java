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

public class MapValueProcessor<T, U, V> extends PipelineStep<Map<T, U>, Map<T, V>> {

    private final PipelineStep<U, V> processor;
    private final Function<PipelineResult<V>, PipelineResult<V>> failureHandling;

    public MapValueProcessor(PipelineStep<U, V> processor) {
        this.processor = processor;
        this.failureHandling = Function.identity();
    }

    /**
     * Class constructor with additional failure handling possibility.
     *
     * @param processor Process each input.
     * @param failureHandling Handle failures explicitly.
     */
    public MapValueProcessor(PipelineStep<U, V> processor, Function<PipelineResult<V>, PipelineResult<V>> failureHandling) {
        this.processor = processor;
        this.failureHandling = failureHandling;
    }

    @Override
    public PipelineResult<Map<T, V>> apply(Map<T, U> tuMap) {
        // Map<T, PResult<V>> -> PResult<Map<T, V>>
        List<PipelineResult<V>> results = new LinkedList<>();
        var contents = new HashMap<T, V>();
        tuMap.forEach((t, u) -> {
            var processingResult = processor.apply(u);
            var failureHandledResult = failureHandling.apply(processingResult);
            results.add(failureHandledResult);
            if (failureHandledResult != null && processingResult.getContent() != null) {
                contents.put(t, processingResult.getContent());
            }
        });

        return Objects.requireNonNullElseGet(handleError(results, contents), () -> PipelineResult.success(contents));
    }
}
