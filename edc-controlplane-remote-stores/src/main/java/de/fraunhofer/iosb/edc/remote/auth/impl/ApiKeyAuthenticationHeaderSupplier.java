package de.fraunhofer.iosb.edc.remote.auth.impl;

import de.fraunhofer.iosb.edc.remote.auth.AuthenticationHeaderSupplier;

import java.util.AbstractMap;
import java.util.Map;

public class ApiKeyAuthenticationHeaderSupplier implements AuthenticationHeaderSupplier {

    private final String apiKey;

    public ApiKeyAuthenticationHeaderSupplier(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public Map.Entry<String, String> getHeader() {
        return new AbstractMap.SimpleEntry<>("x-api-key", apiKey);
    }
}
