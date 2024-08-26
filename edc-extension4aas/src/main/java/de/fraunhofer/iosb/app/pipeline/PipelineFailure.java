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

import org.eclipse.edc.spi.result.Failure;

import java.util.List;

public class PipelineFailure extends Failure {

    private final Type failureType;

    private PipelineFailure(List<String> messages, Type failureType) {
        super(messages);
        this.failureType = failureType;
    }

    /**
     * Create a fatal PipelineFailure. {@link Type#FATAL}
     *
     * @param messages Messages to display by a failure handling mechanism.
     * @return A new fatal PipelineFailure
     */
    public static PipelineFailure fatal(List<String> messages) {
        return new PipelineFailure(messages, Type.FATAL);
    }

    /**
     * Create a warning PipelineFailure. {@link Type#WARNING}
     *
     * @param messages Messages to display by a failure handling mechanism.
     * @return A new warning PipelineFailure
     */
    public static PipelineFailure warning(List<String> messages) {
        return new PipelineFailure(messages, Type.WARNING);
    }

    /**
     * Create an informational PipelineFailure. {@link Type#INFO}
     *
     * @param messages Messages to display by a failure handling mechanism.
     * @return A new informational PipelineFailure
     */
    public static PipelineFailure info(List<String> messages) {
        return new PipelineFailure(messages, Type.INFO);
    }

    /**
     * Get the failure type of this PipelineFailure.
     *
     * @return One of {@link Type}
     */
    public Type getFailureType() {
        return failureType;
    }

    /**
     * One of {@link Type#FATAL}, {@link Type#WARNING}, or {@link Type#INFO}
     */
    public enum Type {
        /**
         * Cannot continue computation (halts pipeline)
         */
        FATAL,
        /**
         * Something unexpected happened
         */
        WARNING,
        /**
         * Something happened
         */
        INFO
    }
}
