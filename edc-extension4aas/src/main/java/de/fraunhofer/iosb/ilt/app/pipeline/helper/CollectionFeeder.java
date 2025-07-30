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
import de.fraunhofer.iosb.ilt.app.pipeline.util.PipelineUtils;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

import static de.fraunhofer.iosb.ilt.app.pipeline.util.PipelineUtils.extractContents;

public class CollectionFeeder<T, U> extends PipelineStep<Collection<T>, Collection<U>> {

    private final PipelineStep<T, U> processor;
    private final Function<Collection<PipelineResult<U>>, PipelineResult<Collection<U>>> failureHandling;

    public CollectionFeeder(final PipelineStep<T, U> processor) {
        this.processor = processor;
        this.failureHandling = PipelineUtils::handleError;
    }

    public CollectionFeeder(final PipelineStep<T, U> processor,
                            Function<Collection<PipelineResult<U>>, PipelineResult<Collection<U>>> failureHandling) {
        this.processor = processor;
        this.failureHandling = failureHandling;
    }

    @Override
    public PipelineResult<Collection<U>> apply(Collection<T> ts) {
        // Results: Collection<PipelineResult<U>>. We want PipelineResult<Collection<U>>
        var results = ts.stream().map(processor).toList();
        var contents = extractContents(results);

        return Objects.requireNonNullElseGet(failureHandling.apply(results), () -> PipelineResult.success(contents));
    }

    @Override
    public String getName() {
        return "CollectionFeeder[%s]".formatted(processor.getName());
    }
}