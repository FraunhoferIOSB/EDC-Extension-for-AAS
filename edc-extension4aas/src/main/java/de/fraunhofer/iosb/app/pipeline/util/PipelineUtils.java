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

public class PipelineUtils {

    private PipelineUtils() {
        throw new RuntimeException("Utility class");
    }

    public static <U, V> PipelineResult<V> handleError(Collection<PipelineResult<U>> results, V contents) {
        PipelineResult<V> pipeResult = null;
        if (results.stream().anyMatch(PipelineResult::failed)) {
            var failureMessages = collectFailureMessages(results);
            pipeResult = switch (maxFailure(results)) {
                case FATAL -> PipelineResult.failure(PipelineFailure.fatal(failureMessages));
                case WARNING -> PipelineResult.negligibleFailure(contents, PipelineFailure.warning(failureMessages));
                case INFO -> PipelineResult.negligibleFailure(contents, PipelineFailure.info(failureMessages));
            };
        }
        return pipeResult;
    }

    public static <U> List<String> collectFailureMessages(Collection<PipelineResult<U>> results) {
        return results.stream()
                .map(AbstractResult::getFailure)
                .filter(Objects::nonNull)
                .map(Failure::getMessages)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .toList();
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

    public static <U> PipelineFailure.Type maxFailure(Collection<PipelineResult<U>> failures) {
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
