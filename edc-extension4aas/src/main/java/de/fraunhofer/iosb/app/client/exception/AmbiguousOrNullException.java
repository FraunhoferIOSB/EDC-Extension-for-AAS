package de.fraunhofer.iosb.app.client.exception;

import org.eclipse.edc.spi.EdcException;

/**
 * Thrown when 
 * 
 * an operation returns ambiguous or no elements as a result when it
 * should have exactly one.
 */
public class AmbiguousOrNullException extends EdcException {

    public AmbiguousOrNullException(String message) {
        super(message);
    }

}
