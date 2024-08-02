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

public class Pipeline<I, O> implements Runnable {

    private final List<PipelineStep<?, ?>> pipelineSteps;
    private final Monitor monitor;


    Pipeline(List<PipelineStep<?, ?>> pipelineSteps, Monitor monitor) {
        this.pipelineSteps = pipelineSteps;
        this.monitor = monitor;
    }

    @SuppressWarnings("unchecked")
    public O execute(I input) {
        Object intermediateResult = input;
        for (var step : pipelineSteps) {
            var result = ((PipelineStep<Object, Object>) step).apply(intermediateResult);
            if (result.succeeded()) {
                intermediateResult = result.getContent();
            } else {
                switch (result.getFailure().getFailureType()) {
                    case FATAL:
                        throw new EdcException("Pipeline received a fatal error: %s".formatted(result.getFailure().getMessages()));
                    case WARNING:
                        monitor.warning("Pipeline received a warning from a pipeline step: %s".formatted(result.getFailure().getMessages()));
                        intermediateResult = result.getContent();
                        continue;
                    case INFO:
                        monitor.info("Pipeline received info from a pipeline step: %s".formatted(result.getFailure().getMessages()));
                        intermediateResult = result.getContent();
                        continue;
                    default:
                        throw new IllegalStateException("Unexpected failure type: " + result.getFailure().getFailureType());
                }
            }
        }
        return (O) intermediateResult;
    }

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
            // TODO think about putting supplier as starting point for pipeline in AasExtension.java
            // Transform supplier to function ignoring input
            var step = new PipelineStep<O, N>() {
                @Override
                public PipelineResult<N> execute(O o) {
                    return PipelineResult.success(supplier.get());
                }
            };

            return step(step);
        }

        public Builder<I, O> monitor(Monitor monitor) {
            this.monitor = monitor;
            return this;
        }

        public <N> Builder<I, N> step(PipelineStep<O, N> step) {
            steps.add(step);
            return new Builder<I, N>(steps);
        }

        public Pipeline<I, O> build() {
            monitor = Objects.requireNonNullElse(monitor, new ConsoleMonitor());
            // From the build process we know that PipelineSteps has I and O as input/output
            return new Pipeline<>(steps, monitor);
        }
    }

}
