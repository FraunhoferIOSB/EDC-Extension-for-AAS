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

import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PipelineTest {

    Pipeline<?, ?> testSubject;

    @Test
    void testEmptyPipeline() {
        testSubject = new Pipeline.Builder<>()
                .monitor(new ConsoleMonitor())
                .build();
        assertNull(testSubject.execute(null));
    }

    @Test
    void testSimplePipeline() {
        testSubject = new Pipeline.Builder<Void, Void>()
                .monitor(new ConsoleMonitor())
                .supplier(() -> true)
                .step(PipelineStep.create(bool -> !bool))
                .build();

        assertEquals(false, testSubject.execute(null));
    }
}