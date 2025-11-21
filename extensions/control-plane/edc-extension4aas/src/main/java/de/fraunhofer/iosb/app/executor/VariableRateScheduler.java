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
package de.fraunhofer.iosb.app.executor;

import de.fraunhofer.iosb.app.model.configuration.Configuration;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;


/**
 * Schedules a list of tasks at a variable rate defined by the {@link Configuration}. At each iteration (defined by the rate), all *finished* tasks are collected and called by
 * their run() function. When a task finishes, it will be called again in the next iteration.
 */
public class VariableRateScheduler extends ScheduledThreadPoolExecutor {

    private final Monitor monitor;
    private final Queue<Runnable> readyForNextIteration = new ConcurrentLinkedQueue<>();
    private final List<Runnable> toRemove = new ArrayList<>();
    private final Supplier<Integer> rateSupplier = () -> Configuration.getInstance().getSyncPeriod();

    private volatile boolean terminateScheduler;


    /**
     * Initialize a VariableRateScheduler.
     *
     * @param corePoolSize Same as in the superclass.
     * @param monitor Logging
     */
    public VariableRateScheduler(int corePoolSize, Monitor monitor) {
        super(corePoolSize);
        this.monitor = monitor;
    }


    /**
     * Adds a runnable to this scheduler. It will be run in the next task iteration.
     *
     * @param runnable The runnable to be added to this scheduler.
     */
    public void addRunnable(Runnable runnable) {
        readyForNextIteration.add(runnable);
    }


    /**
     * Places this runnable in the removal queue. In the next task iteration, this task will get removed from this scheduler. If this task is currently running or was scheduled to
     * run before the removeRunnable call, it will continue executing.
     *
     * @param runnable The runnable to remove from this scheduler
     */
    public void removeRunnable(Runnable runnable) {
        toRemove.add(runnable);
    }


    /**
     * Execute the runnable task at a rate supplied by the second argument. The initial delay is also the supplied rate. Blocking of different AAS services is avoided by enqueuing
     * each service only after it has finished its processing.
     */
    public void run() {
        if (terminateScheduler) {
            monitor.debug("Scheduler stopped execution.");
            return;
        }

        int delay = Math.max(0, rateSupplier.get());
        schedule(() -> {
            try {
                // Drain the ready list to form this iteration's batch
                List<Runnable> batch = getReadyList();

                // Submit all tasks in this iteration.
                // When a task completes, put it back into readyForNextIteration
                batch.forEach(task -> execute(wrapForRequeue(task)));

            }
            catch (Exception e) {
                throw new EdcException("Scheduler: stopping execution exceptionally.", e);
            }

            // Schedule the next iteration
            run();
        }, delay, TimeUnit.SECONDS);
    }


    private List<Runnable> getReadyList() {
        List<Runnable> batch = new ArrayList<>();
        Runnable r;
        while ((r = readyForNextIteration.poll()) != null) {
            if (toRemove.remove(r)) {
                continue;
            }
            batch.add(r);
        }
        return batch;
    }


    private Runnable wrapForRequeue(Runnable task) {
        return () -> {
            try {
                task.run();
            }
            finally {
                // Eligible for the next iteration
                readyForNextIteration.add(task);
            }
        };
    }


    /**
     * Stops this scheduler. If tasks are running, they will finish execution.
     */
    public void terminate() {
        terminateScheduler = true;
        shutdown();
    }

}
