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

    public static PipelineFailure fatal(List<String> messages) {
        return new PipelineFailure(messages, Type.FATAL);
    }

    public static PipelineFailure warning(List<String> messages) {
        return new PipelineFailure(messages, Type.WARNING);
    }

    public static PipelineFailure info(List<String> messages) {
        return new PipelineFailure(messages, Type.INFO);
    }

    public Type getFailureType() {
        return failureType;
    }

    public enum Type {
        /**
         * Cannot continue computation (halts pipeline)
         */
        FATAL,
        /**
         * Something bad happened
         */
        WARNING,
        /**
         * Something unexpected happened
         */
        INFO
    }

}
