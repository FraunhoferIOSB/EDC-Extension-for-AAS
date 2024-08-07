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
package de.fraunhofer.iosb.app.pipeline;

import java.util.List;
import java.util.function.Function;

public abstract class PipelineStep<T, U> implements Function<T, PipelineResult<U>> {

    protected PipelineStep() {
    }

    @Override
    public PipelineResult<U> apply(final T t) {
        try {
            return this.apply(t);
        } catch (Exception e) {
            // If the pipeline step can't recover from its exception we should halt the pipeline
            return PipelineResult.failure(PipelineFailure.fatal(List.of(e.getMessage())));
        }
    }

    public static <T, U> PipelineStep<T, U> create(Function<T, U> func) {
        return new PipelineStep<>() {
            @Override
            public PipelineResult<U> apply(T t) {
                return PipelineResult.success(func.apply(t));
            }
        };
    }
}
