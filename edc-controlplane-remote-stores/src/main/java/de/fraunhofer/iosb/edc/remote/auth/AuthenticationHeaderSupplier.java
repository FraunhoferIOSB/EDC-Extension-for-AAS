package de.fraunhofer.iosb.edc.remote.auth;

import java.util.Map;

public interface AuthenticationHeaderSupplier {
    Map.Entry<String, String> getHeader();
}
