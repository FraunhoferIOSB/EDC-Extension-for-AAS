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
package de.fraunhofer.iosb.ilt.app.pipeline.helper;

import de.fraunhofer.iosb.ilt.app.pipeline.PipelineResult;
import de.fraunhofer.iosb.ilt.app.pipeline.PipelineStep;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.fraunhofer.iosb.ilt.app.pipeline.util.PipelineUtils.extractContents;
import static de.fraunhofer.iosb.ilt.app.pipeline.util.PipelineUtils.handleError;

/**
 * Apply a pipeline step to the input collection and zip input/processed pairs as outputs.
 * Additionally, operations can be performed on the input.
 * Example: A list of URLs, fetch the corresponding websites and return URL as string and website.
 * Input: < URL >. Output < String, String >. keyManipulator: URL::toString
 *
 * @param <T> The input type
 * @param <U> The type of the transformed input
 * @param <V> The processed type
 */
public class InputOutputZipper<T, U, V> extends PipelineStep<Collection<T>, Map<U, V>> {

    private final PipelineStep<T, V> processor;
    private final Function<T, U> keySupplier;

    public InputOutputZipper(final PipelineStep<T, V> processor, Function<T, U> keyManipulator) {
        this.keySupplier = keyManipulator;
        this.processor = processor;
    }

    @Override
    public PipelineResult<Map<U, V>> apply(Collection<T> ts) {
        // Results are in shape PipelineResult<Collection<U>>. We want PipelineResult<Map<T, U>>
        var results = new HashMap<U, PipelineResult<V>>();
        ts.forEach(t -> results.put(keySupplier.apply(t), processor.apply(t)));

        var contents = results.entrySet().stream()
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(),
                        extractContents(List.of(entry.getValue())).stream()
                                .findFirst()
                                .orElse(null)))
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return Objects.requireNonNullElseGet(handleError(results.values(), contents),
                () -> PipelineResult.success(contents));
    }

    @Override
    public String getName() {
        return "InputOutputZipper[%s]".formatted(processor.getName());
    }
}
