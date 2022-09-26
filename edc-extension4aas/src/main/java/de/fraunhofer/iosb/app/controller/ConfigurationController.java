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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import de.fraunhofer.iosb.app.Logger;
import de.fraunhofer.iosb.app.RequestType;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import jakarta.ws.rs.core.Response;

import java.net.URL;

/**
 * Handles requests regarding the application's configuration.
 */
public class ConfigurationController implements Controllable {

    private final Logger logger;
    private Configuration configuration;
    private final ObjectMapper objectMapper;
    private final ObjectReader objectReader;

    public ConfigurationController() {
        logger = Logger.getInstance();
        configuration = Configuration.getInstance();
        objectMapper = new ObjectMapper();
        objectReader = objectMapper.readerForUpdating(configuration);
    }

    @Override
    public Response handleRequest(RequestType requestType, URL url, String... requestData) {
        switch (requestType) {
            case GET:
                return readConfiguration();
            case PUT:
                return updateConfiguration(requestData[0]);
            default:
                return Response.status(Response.Status.NOT_IMPLEMENTED).build();
        }
    }

    private Response readConfiguration() {
        String serializedConfiguration;
        try {
            serializedConfiguration = objectMapper.writeValueAsString(configuration);
        } catch (JsonProcessingException jsonProcessingException) {
            logger.error("Serialization of configuration object failed.\n", jsonProcessingException);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.status(Response.Status.OK).entity(serializedConfiguration).build();
    }

    private Response updateConfiguration(String newConfigValues) {
        // A string containing the new configuration for the response
        String newConfigurationAsString;
        try {
            configuration = objectReader.readValue(newConfigValues);
            newConfigurationAsString = objectMapper.writeValueAsString(configuration);
        } catch (JsonProcessingException jsonProcessingException) {
            logger.error("Updating configuration to this configuration failed:\n" + newConfigValues,
                    jsonProcessingException);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        return Response.status(Response.Status.OK).entity(newConfigurationAsString).build();
    }

}
