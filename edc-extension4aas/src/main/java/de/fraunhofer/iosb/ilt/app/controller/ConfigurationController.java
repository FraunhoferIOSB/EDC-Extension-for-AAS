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
package de.fraunhofer.iosb.ilt.app.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;
import de.fraunhofer.iosb.ilt.app.model.configuration.Configuration;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;

import java.util.Objects;

/**
 * Handles requests regarding the application's configuration.
 */
@Path("/config")
public class ConfigurationController {

    private final Monitor monitor;
    private final Config sysConfig;
    private final ObjectMapper objectMapper;
    private final ObjectReader objectReader;
    private Configuration configuration;

    public ConfigurationController(Config config, Monitor monitor) {
        this.sysConfig = config;
        this.monitor = monitor;
        configuration = Configuration.getInstance();
        objectMapper = JsonMapper.builder().configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true).build();
        objectReader = objectMapper.readerForUpdating(configuration);

        initializeConfiguration();
    }

    private void initializeConfiguration() {
        try {
            configuration = objectReader.readValue(objectMapper.writeValueAsString(sysConfig.getEntries()));
        } catch (JsonProcessingException jsonProcessingException) {
            monitor.severe("Initializing AAS extension configuration failed",
                    jsonProcessingException);
        }

    }

    /**
     * Return the current configuration values of this extension.
     *
     * @return Current configuration values
     */
    @GET
    public Response getConfiguration() {
        monitor.info("GET /config");

        try {
            var serializedConfiguration = objectMapper.writeValueAsString(configuration);
            return Response.status(Response.Status.OK).entity(serializedConfiguration).build();
        } catch (JsonProcessingException jsonProcessingException) {
            monitor.severe("Serialization of configuration object failed.\n", jsonProcessingException);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update the current configuration.
     *
     * @param newConfigValues New configuration values as JSON string
     * @return Response with status code
     */
    @PATCH
    public Response updateConfiguration(String newConfigValues) {
        monitor.info("PATCH /config");

        if (Objects.isNull(newConfigValues)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing request body").build();
        }

        try {
            // Read config values as map -> edc Config -> merge with old
            // -> set as AAS extension config
            Config newConfig = ConfigFactory.fromMap(objectMapper.readValue(newConfigValues,
                    new TypeReference<>() {
                    }));
            Config mergedConfig = sysConfig.merge(newConfig);
            configuration = objectReader.readValue(objectMapper.writeValueAsString(mergedConfig.getEntries()));
        } catch (JsonProcessingException jsonProcessingException) {
            monitor.severe("Updating configuration to this configuration failed:\n" + newConfigValues,
                    jsonProcessingException);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        return Response.status(Response.Status.OK).build();
    }
}
