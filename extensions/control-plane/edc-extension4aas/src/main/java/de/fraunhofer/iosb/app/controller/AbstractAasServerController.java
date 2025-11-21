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
package de.fraunhofer.iosb.app.controller;

import de.fraunhofer.iosb.app.executor.VariableRateScheduler;
import de.fraunhofer.iosb.app.handler.edc.EdcStoreHandler;
import de.fraunhofer.iosb.app.stores.repository.AasServerStore;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.spi.monitor.Monitor;

import java.net.URI;


public abstract class AbstractAasServerController {
    protected static final String FAILURE_TEMPLATE = "Failed registering: %s";
    protected static final String CONNECT_EXCEPTION_TEMPLATE = "Could not connect to %s";
    protected static final String UNAUTHORIZED_EXCEPTION_TEMPLATE = "Not authorized to connect to %s";
    protected static final String NOT_FOUND_TEMPLATE = "%s not found in registered AAS servers.";
    protected static final String EXISTS_TEMPLATE = "%s is already a registered server";

    protected final Monitor monitor;
    protected final AasServerStore aasServerStore;
    // Manages remote AAS servers by polling their API
    protected final VariableRateScheduler scheduler;
    protected final EdcStoreHandler edcStoreHandler;


    protected AbstractAasServerController(Monitor monitor, AasServerStore aasServerStore, VariableRateScheduler scheduler, EdcStoreHandler edcStoreHandler) {
        this.monitor = monitor;
        this.aasServerStore = aasServerStore;
        this.edcStoreHandler = edcStoreHandler;
        this.scheduler = scheduler;
        this.scheduler.run();
    }


    protected Response badRequest(String template, URI uri) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(String.format(String.format(FAILURE_TEMPLATE, template), uri))
                .build();
    }


    protected abstract void unregister(URI uri);


    /**
     * Stops all internally started AAS services
     */
    public void unregisterAll() {
        monitor.debug("Stopping all internally started AAS services...");
        aasServerStore.keySet().forEach(this::unregister);
        scheduler.terminate();
    }
}
