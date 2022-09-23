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
