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

import com.fasterxml.jackson.core.JsonProcessingException;
import de.fraunhofer.iosb.aas.lib.model.impl.Service;
import de.fraunhofer.iosb.app.model.aas.registry.RegistryRepository;
import de.fraunhofer.iosb.app.model.aas.service.ServiceRepository;
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionSerializer;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.spi.monitor.Monitor;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static de.fraunhofer.iosb.app.controller.SelfDescriptionController.SELF_DESCRIPTION_PATH;

/**
 * Handles requests to the selfDescription endpoint.
 */
@Produces({ MediaType.APPLICATION_JSON })
@Path(SELF_DESCRIPTION_PATH)
public class SelfDescriptionController {

    public static final String SELF_DESCRIPTION_PATH = "selfDescription";

    private final Monitor monitor;
    private final ServiceRepository serviceRepository;
    private final RegistryRepository registryRepository;

    /**
     * Class constructor
     *
     * @param monitor            Logs
     * @param serviceRepository  Read services for self-description
     * @param registryRepository Read registries for self-description
     */
    public SelfDescriptionController(Monitor monitor, ServiceRepository serviceRepository,
                                     RegistryRepository registryRepository) {
        this.monitor = monitor;
        this.serviceRepository = serviceRepository;
        this.registryRepository = registryRepository;
    }

    /**
     * Print self-descriptions of AAS environments registered at this EDC. If no
     * query parameter is given, print all self-descriptions available.
     *
     * @param aasServiceUrl Specify an AAS environment by its service
     * @return Self description(s)
     */
    @GET
    public Response getSelfDescription(@QueryParam("url") URL aasServiceUrl) throws JsonProcessingException {
        if (aasServiceUrl == null) {
            monitor.debug(String.format("GET /%s", SELF_DESCRIPTION_PATH));
            return Response.ok(getAllSelfDescriptions()).build();
        }

        monitor.debug(String.format("GET /%s/%s", SELF_DESCRIPTION_PATH, aasServiceUrl));
        var registry = registryRepository.getEnvironments(aasServiceUrl);

        if (registry != null) {
            var environments = registry.stream()
                    .map(Service::environment)
                    .toList();

            return Response.ok(environmentsAsSelfDescriptionString(environments)).build();
        }

        var service = serviceRepository.getEnvironment(aasServiceUrl);

        if (service != null) {
            return Response.ok(environmentsAsSelfDescriptionString(List.of(service))).build();
        }

        monitor.warning("URL %s not found.".formatted(aasServiceUrl));
        return Response.status(Response.Status.NOT_FOUND).build();
    }


    private String getAllSelfDescriptions() throws JsonProcessingException {
        // Service and Registry environments can be null if they have not been fetched yet
        var environments = new ArrayList<>(serviceRepository.getAllEnvironments());

        environments.addAll(registryRepository.getAllEnvironments().stream()
                .map(Service::environment)
                .toList());

        return environmentsAsSelfDescriptionString(environments);
    }

    private String environmentsAsSelfDescriptionString(List<Asset> environments) throws JsonProcessingException {
        var selfDescription = new ArrayList<String>();
        for (Asset environment : environments) {
            if (environment == null) {
                continue;
            }
            String s = SelfDescriptionSerializer.assetToString(environment);
            selfDescription.add(s);
        }

        return intoJsonArray(selfDescription.stream());
    }


    private String intoJsonArray(Stream<String> contents) {
        return "[%s]".formatted(contents.reduce("%s,%s"::formatted).orElse(null));
    }
}
