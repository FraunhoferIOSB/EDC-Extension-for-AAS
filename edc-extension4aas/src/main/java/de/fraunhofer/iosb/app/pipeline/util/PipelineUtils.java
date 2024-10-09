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
package de.fraunhofer.iosb.app.pipeline.util;

import de.fraunhofer.iosb.app.pipeline.PipelineFailure;
import de.fraunhofer.iosb.app.pipeline.PipelineResult;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Failure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Utility class with error handling, failure message collecting and content extracting capabilities for collections
 * of pipeline results.
 */
public class PipelineUtils {

    private PipelineUtils() {
        throw new RuntimeException("Utility class");
    }

    /**
     * Handles errors of a collection of pipeline results.
     * <p>
     * <b>If no failure occurred, null is returned!</b>
     * <li>The pipeline result will have as error type the gravest of errors occurring in the input result collection
     * .</li>
     * <li>The pipeline result will have as failure message all the failure messages of the input results.</li>
     * <li>The pipeline result will have as content the content from the input pipeline results if no fatal error
     * occurred.</li>
     *
     * @param results Pipeline result collection containing zero, one or multiple failures.
     * @param <U>     The content type
     * @return The pipeline result as described above, or null.
     */
    public static <U> PipelineResult<Collection<U>> handleError(Collection<PipelineResult<U>> results) {
        PipelineResult<Collection<U>> pipeResult = null;
        if (results.stream().anyMatch(AbstractResult::failed)) {
            var failureMessages = collectFailureMessages(results);
            pipeResult = switch (maxFailure(results)) {
                case FATAL -> PipelineResult.failure(PipelineFailure.fatal(failureMessages));
                case WARNING -> PipelineResult.recoverableFailure(extractContents(results),
                        PipelineFailure.warning(failureMessages));
                case INFO -> PipelineResult.recoverableFailure(extractContents(results),
                        PipelineFailure.info(failureMessages));
            };
        }
        return pipeResult;
    }

    /**
     * Handles errors of a collection of pipeline results.
     * <p>
     * <b>If no failure occurred, null is returned!</b>
     * <li>The pipeline result will have as error type the gravest of errors occurring in the input result collection
     * .</li>
     * <li>The pipeline result will have as failure message all the failure messages of the input results.</li>
     * <li>The pipeline result will have as content the content from the input parameters if no fatal error occurred
     * .</li>
     *
     * @param results  Pipeline result collection containing zero, one or multiple failures.
     * @param contents Contents of the input pipeline results which are to be returned in the pipeline result.
     * @param <U>      The content type
     * @param <V>      The Content type
     * @return The pipeline result as described above, or null.
     */
    public static <U, V> PipelineResult<V> handleError(Collection<PipelineResult<U>> results, V contents) {
        PipelineResult<V> pipeResult = null;
        if (results.stream().anyMatch(PipelineResult::failed)) {
            var failureMessages = collectFailureMessages(results);
            pipeResult = switch (maxFailure(results)) {
                case FATAL -> PipelineResult.failure(PipelineFailure.fatal(failureMessages));
                case WARNING -> PipelineResult.recoverableFailure(contents, PipelineFailure.warning(failureMessages));
                case INFO -> PipelineResult.recoverableFailure(contents, PipelineFailure.info(failureMessages));
            };
        }
        return pipeResult;
    }


    /**
     * Get non-null contents from the results.
     *
     * @param results The results of a pipeline step containing contents
     * @param <U>     Content type
     * @return The non-null contents
     */
    public static <U> Collection<U> extractContents(Collection<PipelineResult<U>> results) {
        return results.stream()
                .map(AbstractResult::getContent)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static <U> List<String> collectFailureMessages(Collection<PipelineResult<U>> results) {
        return results.stream()
                .map(AbstractResult::getFailure)
                .filter(Objects::nonNull)
                .map(Failure::getMessages)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .toList();
    }

    private static <U> PipelineFailure.Type maxFailure(Collection<PipelineResult<U>> failures) {
        var failureTypes = failures.stream()
                .map(AbstractResult::getFailure)
                .filter(Objects::nonNull)
                .map(PipelineFailure::getFailureType)
                .toList();

        if (failureTypes.stream().anyMatch(PipelineFailure.Type.FATAL::equals)) {
            return PipelineFailure.Type.FATAL;
        } else if (failureTypes.stream().anyMatch(PipelineFailure.Type.WARNING::equals)) {
            return PipelineFailure.Type.WARNING;
        } else if (failureTypes.stream().anyMatch(PipelineFailure.Type.INFO::equals)) {
            return PipelineFailure.Type.INFO;
        }
        throw new IllegalArgumentException("Cannot compute max failure type without failures");
    }
}
