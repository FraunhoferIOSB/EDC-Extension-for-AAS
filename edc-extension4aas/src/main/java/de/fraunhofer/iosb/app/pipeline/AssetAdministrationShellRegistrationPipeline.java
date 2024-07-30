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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class AssetAdministrationShellRegistrationPipeline<I, O> implements Runnable {

    private final List<PipelineStep<?, ?>> pipelineSteps;

    AssetAdministrationShellRegistrationPipeline(List<PipelineStep<?, ?>> pipelineSteps) {
        this.pipelineSteps = pipelineSteps;
    }

    @SuppressWarnings("unchecked")
    public O execute(I input) {
        Object intermediateResult = input;
        for (var step : pipelineSteps) {
            var result = ((PipelineStep<Object, Object>) step).apply(intermediateResult);
            if (result.succeeded()) {
                intermediateResult = result.getContent();
            } else {
                throw new EdcException("Pipeline step failed");
            }
        }
        return (O) intermediateResult;
    }

    @Override
    public void run() {
        if (!pipelineSteps.isEmpty() && pipelineSteps.get(0).getInput().equals(Void.class)) {
            execute(null);
        } else {
            throw new IllegalArgumentException("Cannot start this pipeline without any input");
        }
    }

    public static class Builder<I, O> {

        private final List<PipelineStep<?, ?>> pipelineSteps;

        public Builder() {
            pipelineSteps = new ArrayList<>();
        }

        private Builder(List<PipelineStep<?, ?>> instance) {
            pipelineSteps = instance;
        }

        /**
         * Append supplier to pipeline. This step will ignore previous step's output if any.
         */
        public <N> Builder<I, N> supplier(Supplier<N> supplier) {
            // TODO think about putting supplier as starting point for pipeline in AasExtension.java
            // Transform supplier to function ignoring input
            var step = new PipelineStep<O, N>() {
                @Override
                public N execute(O o) {
                    return supplier.get();
                }
            };

            return step(step);
        }

        public <N> Builder<I, N> step(PipelineStep<O, N> step) {
            var canHandle = pipelineSteps.get(pipelineSteps.size() - 1).canHandle(step.getInput());

            if (canHandle) {
                pipelineSteps.add(step);
                return new Builder<I, N>(pipelineSteps);
            }

            throw new EdcException("Output of this step does not match output type of last step");
        }

        public AssetAdministrationShellRegistrationPipeline<I, O> build() {
            // From the build process we know that PipelineSteps has I and O as input/output
            return new AssetAdministrationShellRegistrationPipeline<>(pipelineSteps);
        }
    }

}
