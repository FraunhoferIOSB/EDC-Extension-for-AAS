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

import de.fraunhofer.iosb.app.aas.mapper.util.FilteredJsonSerializer;
import de.fraunhofer.iosb.app.handler.aas.AasHandler;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.app.stores.repository.AasServerStore;
import de.fraunhofer.iosb.client.exception.UnauthorizedException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.edc.spi.monitor.Monitor;

import java.net.ConnectException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static de.fraunhofer.iosb.app.controller.SelfDescriptionController.SELF_DESCRIPTION_PATH;
import static de.fraunhofer.iosb.constants.AasConstants.DEFAULT_EXPOSED_FIELDS;
import static de.fraunhofer.iosb.constants.AasConstants.EXPOSED_FIELDS_SELF_DESCRIPTION;


/**
 * Handles requests to the selfDescription endpoint.
 */
@Produces({ MediaType.APPLICATION_JSON })
@Path(SELF_DESCRIPTION_PATH)
public class SelfDescriptionController {

    public static final String SELF_DESCRIPTION_PATH = "selfDescription";

    private final Supplier<Set<String>> exposedFieldsSupplier = () -> Optional.ofNullable(Configuration.getInstance().getExposedFields())
            .orElse(DEFAULT_EXPOSED_FIELDS);
    private final Monitor monitor;
    private final AasServerStore aasRepositoryStore;
    private final FilteredJsonSerializer filteredJsonSerializer = new FilteredJsonSerializer();


    /**
     * Class constructor
     *
     * @param monitor Logs
     * @param aasRepositoryStore AAS repository store
     */
    public SelfDescriptionController(Monitor monitor, AasServerStore aasRepositoryStore) {
        this.monitor = monitor;
        this.aasRepositoryStore = aasRepositoryStore;
    }


    /**
     * Print self-descriptions of AAS environments registered at this EDC. If no query parameter is given, print all self-descriptions available.
     *
     * @param uri Specify an AAS environment by its service
     * @return Self description(s)
     */
    @GET
    public String getSelfDescription(@QueryParam("url") URI uri) {
        monitor.debug(String.format("GET %s", SELF_DESCRIPTION_PATH));

        List<AasHandler<?>> handlers = new ArrayList<>();

        Optional.ofNullable(uri)
                .ifPresentOrElse(u ->
                                handlers.addAll(aasRepositoryStore.get(u)
                                        .map(List::of)
                                        // If no uri is supplied, return all SelfDescriptions
                                        .orElseThrow(() -> new BadRequestException("URI not registered."))),
                        () -> handlers.addAll(aasRepositoryStore.getAll())
                );

        List<Environment> selfDescriptions = new ArrayList<>();
        for (AasHandler<?> handler: handlers) {
            try {
                selfDescriptions.add(handler.buildSelfDescription());
            }
            catch (UnauthorizedException | ConnectException e) {
                monitor.warning("Could not produce a self description", e);
            }
        }

        Set<String> exposedFields = new HashSet<>(exposedFieldsSupplier.get());
        exposedFields.addAll(EXPOSED_FIELDS_SELF_DESCRIPTION);

        return "[" + selfDescriptions.stream()
                .map(selfDescription -> {
                    try {
                        return filteredJsonSerializer.write(selfDescription, exposedFields);
                    }
                    catch (SerializationException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.joining(",")) + "]";
    }
}
