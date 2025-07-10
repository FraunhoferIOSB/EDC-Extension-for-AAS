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

import de.fraunhofer.iosb.aas.lib.model.impl.Registry;
import de.fraunhofer.iosb.aas.lib.model.impl.Service;
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionChangeListener;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Schedules a task at a variable rate with additional external signal triggering.
 */
public class VariableRateScheduler extends ScheduledThreadPoolExecutor implements SelfDescriptionChangeListener {

    private final Monitor monitor;
    private final Runnable runnable;

    private boolean terminateScheduler;

    /**
     * Initialize a VariableRateScheduler, a subtype of the {@link java.util.concurrent.ScheduledThreadPoolExecutor}.
     *
     * @param corePoolSize Same as in the superclass.
     * @param runnable     Runnable to be executed at a variable rate.
     * @param monitor      Logging
     */
    public VariableRateScheduler(int corePoolSize, Runnable runnable, Monitor monitor) {
        super(corePoolSize);
        this.runnable = runnable;
        this.monitor = monitor;
    }

    /**
     * Execute the runnable task at a rate supplied by the second argument. The initial delay is also the supplied rate.
     *
     * @param rateSupplier Provides the variable rate at which the runnable is to be executed.
     */
    public void scheduleAtVariableRate(Supplier<Integer> rateSupplier) {
        if (terminateScheduler) {
            monitor.info("VariableRateScheduler stopped execution.");
            return;
        }
        schedule(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                monitor.severe("VariableRateScheduler stopped execution.", e);
                throw new EdcException(e);
            }
            scheduleAtVariableRate(rateSupplier);
        }, (long) rateSupplier.get(), TimeUnit.SECONDS);
    }

    public void terminate() {
        terminateScheduler = true;
    }

    @Override
    public void created(Registry registry) {
        runnable.run();
    }

    @Override
    public void created(Service service) {
        runnable.run();
    }
}
