package de.fraunhofer.iosb.app.util;

import java.util.Base64;

/**
 * Encoding utility class.
 */
public class Encoder {

    /**
     * Return base64 encoded String version of input
     */
    public static String encodeBase64(String toBeEncoded) {
        return Base64.getEncoder().encodeToString(toBeEncoded.getBytes());
    }
}
