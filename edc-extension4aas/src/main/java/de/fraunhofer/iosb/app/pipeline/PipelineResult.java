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
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Returned by any PipelineStep inside a {@link Pipeline}. Can have content even in failure scenarios (not if failure
 * is fatal).
 *
 * @param <T> Content type
 */
public class PipelineResult<T> extends AbstractResult<T, PipelineFailure, PipelineResult<T>> {

    protected PipelineResult(@Nullable final T content, @Nullable final PipelineFailure failure) {
        super(content, failure);
    }

    public static <T> PipelineResult<T> success(@Nullable final T content) {
        return new PipelineResult<>(content, null);
    }

    /**
     * An irrecoverable failure. This means that the pipeline step produced an erroneous result or nothing at all.
     *
     * @param failure The failure containing messages and a failure type.
     * @param <U>     The content type if the pipeline step succeeded.
     * @return The created PipelineResult.
     */
    public static <U> PipelineResult<U> failure(PipelineFailure failure) {
        return new PipelineResult<>(null, failure);
    }

    /**
     * Can be recovered / ignored. Can be used if content is still usable.
     *
     * @param content Potentially corrupted / malformed result content
     * @param failure PipelineFailure
     * @param <T>     Content type
     * @return the PipelineResult containing failure and content
     */
    public static <T> PipelineResult<T> recoverableFailure(@NotNull final T content, PipelineFailure failure) {
        if (failure.getFailureType().equals(PipelineFailure.Type.FATAL)) {
            throw new IllegalStateException("Can not instantiate recoverable failure with failure signal FATAL");
        }
        return new PipelineResult<>(content, failure);
    }

    /**
     * Create a pipeline result from a store result. Useful if an operation to, for example, the AssetIndex is
     * forwarded as a pipeline result.
     *
     * @param storeResult The corresponding store result.
     * @param <T>         The content type of the store result and the resulting pipeline result
     * @return A pipeline result with the same content or failure messages and a mapping of
     *         {@link org.eclipse.edc.spi.result.StoreFailure.Reason} to {@link PipelineFailure.Type}
     */
    public static <T> PipelineResult<T> from(StoreResult<T> storeResult) {
        if (storeResult.succeeded()) {
            return success(storeResult.getContent());
        }
        return switch (storeResult.getFailure().getReason()) {
            case ALREADY_EXISTS -> failure(PipelineFailure.info(storeResult.getFailureMessages()));
            case DUPLICATE_KEYS, NOT_FOUND -> failure(PipelineFailure.warning(storeResult.getFailureMessages()));
            case GENERAL_ERROR, ALREADY_LEASED -> failure(PipelineFailure.fatal(storeResult.getFailureMessages()));
        };
    }

    /**
     * Returns warning on result failure. See also: from(StoreResult)
     *
     * @param result Result to transform.
     * @param <T>    Content type
     * @return PipelineResult
     */
    public static <T> PipelineResult<T> from(Result<T> result) {
        if (result.succeeded()) {
            return success(result.getContent());
        }
        return failure(PipelineFailure.warning(result.getFailureMessages()));
    }

    /**
     * Return the same pipeline result with new content. The new content can also have a new type.
     *
     * @param content The new content with possibly a new type.
     * @param <N>     The possibly new content type.
     * @return A pipeline result with new content
     */
    public <N> PipelineResult<N> withContent(N content) {
        if (this.succeeded()) {
            return success(content);
        } else {
            return recoverableFailure(content, this.getFailure());
        }
    }

    @NotNull
    @SuppressWarnings("unchecked")
    @Override
    protected <R1 extends AbstractResult<C1, PipelineFailure, R1>, C1> R1 newInstance(@Nullable C1 content,
                                                                                      @Nullable PipelineFailure pipelineFailure) {
        return (R1) new PipelineResult<>(content, pipelineFailure);
    }
}
