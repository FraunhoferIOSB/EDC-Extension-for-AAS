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

import de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository;
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionSerializer;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.spi.monitor.Monitor;

import java.net.URL;
import java.util.Objects;
import java.util.stream.Collectors;

import static de.fraunhofer.iosb.app.controller.SelfDescriptionController.SELF_DESCRIPTION_PATH;

/**
 * Handles requests to the selfDescription endpoint.
 */
@Produces({MediaType.APPLICATION_JSON})
@Path(SELF_DESCRIPTION_PATH)
public class SelfDescriptionController {

    public static final String SELF_DESCRIPTION_PATH = "selfDescription";

    private final Monitor monitor;
    private final SelfDescriptionRepository selfDescriptionRepository;

    /**
     * Class constructor
     *
     * @param monitor                   Logs
     * @param selfDescriptionRepository Manage self descriptions
     */
    public SelfDescriptionController(Monitor monitor, SelfDescriptionRepository selfDescriptionRepository) {
        this.monitor = monitor;
        this.selfDescriptionRepository = selfDescriptionRepository;
    }

    /**
     * Print self-descriptions of AAS environments registered at this EDC. If no
     * query parameter is given, print all self-descriptions available.
     *
     * @param aasServiceUrl Specify an AAS environment by its service
     * @return Self description(s)
     */
    @GET
    public Response getSelfDescription(@QueryParam("aasService") URL aasServiceUrl) {
        if (Objects.isNull(aasServiceUrl)) {
            monitor.debug("Received a self description GET request");

            var selfDescriptionString = selfDescriptionRepository.getAllEnvironments().stream()
                    .filter(Objects::nonNull)
                    .map(SelfDescriptionSerializer::assetToString)
                    .collect(Collectors.joining(","));

            return Response.ok(selfDescriptionString).build();
        } else {
            monitor.debug("Received a self description GET request for %s".formatted(aasServiceUrl));
            var selfDescriptionString = selfDescriptionRepository.getEnvironments(aasServiceUrl.toString()).stream()
                    .filter(Objects::nonNull)
                    .map(SelfDescriptionSerializer::assetToString)
                    .collect(Collectors.joining(","));
            if (!selfDescriptionString.isEmpty()) {
                return Response.ok(selfDescriptionString).build();
            } else {
                monitor.warning("Self description with URL %s not found.".formatted(aasServiceUrl));
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        }
    }
}
