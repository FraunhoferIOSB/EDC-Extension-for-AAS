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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;
import de.fraunhofer.iosb.app.RequestType;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;

import java.net.URL;

/**
 * Handles requests regarding the application's configuration.
 */
public class ConfigurationController implements Controllable {

    private final Monitor monitor;
    private final Config sysConfig;
    private Configuration configuration;
    private final ObjectMapper objectMapper;
    private final ObjectReader objectReader;

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

    @Override
    public Response handleRequest(RequestType requestType, URL url, String... requestData) {
        return switch (requestType) {
            case GET -> getConfiguration();
            case PUT -> updateConfiguration(requestData[0]);
            default -> Response.status(Response.Status.NOT_IMPLEMENTED).build();
        };
    }

    private Response getConfiguration() {
        try {
            var serializedConfiguration = objectMapper.writeValueAsString(configuration);
            return Response.status(Response.Status.OK).entity(serializedConfiguration).build();
        } catch (JsonProcessingException jsonProcessingException) {
            monitor.severe("Serialization of configuration object failed.\n", jsonProcessingException);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Response updateConfiguration(String newConfigValues) {
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
