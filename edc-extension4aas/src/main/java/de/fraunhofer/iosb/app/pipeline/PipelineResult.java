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

import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Failure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PipelineResult<T> extends AbstractResult<T, PipelineFailure, PipelineResult<T>> {

    protected PipelineResult(@Nullable final T content, @Nullable final PipelineFailure failure) {
        super(content, failure);
    }

    public static <T> PipelineResult<T> success(@NotNull final T content) {
        return new PipelineResult<>(content, null);
    }

    public static <U> PipelineResult<U> failure(PipelineFailure failure) {
        return new PipelineResult<>(null, failure);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    @Override
    protected <R1 extends AbstractResult<C1, PipelineFailure, R1>, C1> R1 newInstance(@Nullable C1 content, @Nullable PipelineFailure pipelineFailure) {
        return (R1) new PipelineResult<>(content, pipelineFailure);
    }
}
