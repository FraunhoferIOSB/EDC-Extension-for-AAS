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

import java.util.function.Function;

/**
 * A pipeline step is a part of a pipeline which receives input of type T and produces an output of type
 * PipelineResult< U >
 *
 * @param <T> Input type of the pipeline step
 * @param <U> Output type of the pipeline step
 */
public abstract class PipelineStep<T, U> implements Function<T, PipelineResult<U>> {

    protected PipelineStep() {
    }

    /**
     * Create an ad-hoc pipeline step.
     *
     * @param func The function this pipeline step should execute
     * @param <T>  The input type of the step
     * @param <U>  The output type of the step
     * @return The new pipeline step.
     */
    public static <T, U> PipelineStep<T, U> create(Function<T, U> func) {
        return new PipelineStep<>() {

            @Override
            public String getName() {
                return "anonymous pipeline step";
            }

            @Override
            public PipelineResult<U> apply(T t) {
                return PipelineResult.success(func.apply(t));
            }
        };
    }

    public String getName() {
        return this.getClass().getSimpleName();
    }
}
