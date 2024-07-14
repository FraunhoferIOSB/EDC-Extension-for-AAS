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
package de.fraunhofer.iosb.app.util;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Schedules a task at a variable rate.
 */
public class VariableRateScheduler extends ScheduledThreadPoolExecutor {

    /**
     * Initialize a VariableRateScheduler, a subtype of the {@link java.util.concurrent.ScheduledThreadPoolExecutor}.
     *
     * @param corePoolSize Same as in the superclass.
     */
    public VariableRateScheduler(int corePoolSize) {
        super(corePoolSize);
    }

    /**
     * Execute the runnable task at a rate supplied by the second argument. The initial delay is also the supplied rate.
     *
     * @param runnable     Runnable to be executed at a variable rate.
     * @param rateSupplier Provides the variable rate at which the runnable is to be executed.
     */
    public void scheduleAtVariableRate(Runnable runnable, Supplier<Integer> rateSupplier) {
        schedule(() -> {
            runnable.run();
            scheduleAtVariableRate(runnable, rateSupplier);
        }, (long) rateSupplier.get(), TimeUnit.SECONDS);
    }
}
