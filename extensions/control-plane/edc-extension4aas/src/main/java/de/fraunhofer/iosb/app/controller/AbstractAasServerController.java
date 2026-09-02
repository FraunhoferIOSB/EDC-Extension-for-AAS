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
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;

import java.net.URI;


/**
 * Abstract class bundling functionality to manage AAS servers such as AAS repositories.
 */
public abstract class AbstractAasServerController {
    /** Message template used when a connection to an AAS server fails. */
    protected static final String CONNECT_EXCEPTION_TEMPLATE = "Could not connect to %s";
    /** Message template used when authentication at an AAS server fails. */
    protected static final String UNAUTHORIZED_EXCEPTION_TEMPLATE = "Not authorized to connect to %s";
    /** Message template used when an AAS server could not be found among the registered servers. */
    protected static final String NOT_FOUND_TEMPLATE = "%s not found in registered AAS servers.";
    /** Message template used when an AAS server is already registered. */
    protected static final String EXISTS_TEMPLATE = "%s is already a registered server";

    /** Monitor used for logging. */
    protected final Monitor monitor;
    /** Store holding the registered AAS servers and their handlers. */
    protected final AasServerStore aasServerStore;
    // Manages remote AAS servers by polling their API
    /** Scheduler used to periodically update the state of registered EDC assets. */
    protected final VariableRateScheduler scheduler;
    /** Handler to manage registration of EDC assets, policies and contracts. */
    protected final EdcStoreHandler edcStoreHandler;
    /** Vault used to retrieve secrets for authentication at AAS servers. */
    protected final Vault vault;
    /** OAuth2 client used to obtain tokens for authentication at AAS servers. */
    protected final Oauth2Client oauth2Client;


    /**
     * Default constructor.
     *
     * @param monitor Monitor used for log outputs.
     * @param aasServerStore AAS server store to store AAS servers.
     * @param scheduler scheduler used to update the current state of the registered EDC assets.
     * @param edcStoreHandler EDC store handler to manage registration of EDC assets, policies and contracts.
     * @param vault vault used to get secrets for authentication at AAS servers.
     * @param oauth2Client oauth2 client to get tokens for authentication at AAS servers.
     */
    protected AbstractAasServerController(Monitor monitor, AasServerStore aasServerStore, VariableRateScheduler scheduler, EdcStoreHandler edcStoreHandler, Vault vault,
                                          Oauth2Client oauth2Client) {
        this.monitor = monitor;
        this.aasServerStore = aasServerStore;
        this.edcStoreHandler = edcStoreHandler;
        this.scheduler = scheduler;
        this.vault = vault;
        this.oauth2Client = oauth2Client;
        this.scheduler.run();
    }


    /**
     * Unregister an AAS server.
     *
     * @param uri the URI of the AAS server to unregister.
     */
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
