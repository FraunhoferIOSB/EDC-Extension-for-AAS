package de.fraunhofer.iosb.app.handler.aas.registry;

/**
 * No endpoint was found for an AAS descriptor or submodel descriptor.
 */
public class NoEndpointException extends Exception {
    public NoEndpointException(String s) {
        super(s);
    }
}
