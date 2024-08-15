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

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A pipeline implementation with generic pipeline steps and customizable failure handling.
 *
 * @param <I> Input type of the pipeline
 * @param <O> Output type of the pipeline
 */
public class Pipeline<I, O> implements Runnable {

    private final List<PipelineStep<?, ?>> pipelineSteps;
    private final Monitor monitor;

    protected Pipeline(List<PipelineStep<?, ?>> pipelineSteps, Monitor monitor) {
        this.pipelineSteps = pipelineSteps;
        this.monitor = monitor;
    }

    /**
     * Execute pipeline passing along intermediate results and returning the last result of type O
     *
     * @param input The input to the pipeline
     * @return The final result
     */
    @SuppressWarnings("unchecked")
    public O execute(I input) {
        Object intermediateResult = input;
        for (var step : pipelineSteps) {
            var result = ((PipelineStep<Object, Object>) step).apply(intermediateResult);
            if (result.failed()) {
                intermediateResult = handlePipelineFailure(step, result);
            } else {
                intermediateResult = result.getContent();
            }
        }
        return (O) intermediateResult;
    }

    /**
     * Throw exception on unrecoverable failure (this will halt the execution of the pipeline)
     * or return an acceptable result for the next PipelineStep.
     * The content of the result can be modified by overriding handlePipelineFailure()
     * <p>
     * The default implementation is to throw an exception on FATAL failures and log + return result else
     *
     * @param step   The failed PipelineStep
     * @param result The result coming from the failed PipelineStep
     */
    protected <T> T handlePipelineFailure(PipelineStep<?, ?> step, PipelineResult<T> result) {
        if (result.failed()) {
            doHandle(result.getFailure());
            return result.getContent();
        } else {
            throw new IllegalStateException("Error trying to handle failure when result did not fail");
        }
    }

    private void doHandle(PipelineFailure failure) {
        switch (failure.getFailureType()) {
            case FATAL:
                throw new EdcException("Pipeline received a fatal error: %s".formatted(failure.getMessages()));
            case WARNING:
                monitor.warning("Pipeline received a warning from a pipeline step: %s".formatted(failure.getMessages()));
                break;
            case INFO:
                monitor.info("Pipeline received info from a pipeline step: %s".formatted(failure.getMessages()));
                break;
            default:
                throw new IllegalStateException("Unexpected failure type: " + failure.getFailureType());
        }
    }

    /**
     * Start the pipeline without input argument.
     * This is used for running the pipeline on a separate thread.
     */
    @Override
    public void run() {
        execute(null);
    }

    public static class Builder<I, O> {

        private Monitor monitor;
        private final List<PipelineStep<?, ?>> steps;

        private Builder(List<PipelineStep<?, ?>> instance) {
            steps = instance;
        }

        public Builder() {
            this(new ArrayList<>());
        }

        /**
         * Append supplier to pipeline. This step will ignore previous step's output if any.
         */
        public <N> Builder<I, N> supplier(Supplier<N> supplier) {
            // Transform supplier to function ignoring input
            var step = new PipelineStep<O, N>() {
                @Override
                public PipelineResult<N> apply(O o) {
                    return PipelineResult.success(supplier.get());
                }
            };

            return step(step);
        }

        public Builder<I, O> initialStep(PipelineStep<I, O> step) {
            steps.add(step);
            return this;
        }

        /**
         * Append pipeline step.
         *
         * @param step Next step in the pipeline
         * @param <N>  The new output type of the pipeline
         * @return builder
         */
        public <N> Builder<I, N> step(PipelineStep<O, N> step) {
            steps.add(step);
            // Assure that last.output==next.input
            return new Builder<>(steps);
        }

        public Builder<I, O> monitor(Monitor monitor) {
            this.monitor = monitor;
            return this;
        }

        public Pipeline<I, O> build() {
            monitor = Objects.requireNonNullElseGet(monitor, ConsoleMonitor::new);
            // From the build process we know that PipelineSteps has I and O as input/output
            return new Pipeline<>(steps, monitor);
        }
    }

}
