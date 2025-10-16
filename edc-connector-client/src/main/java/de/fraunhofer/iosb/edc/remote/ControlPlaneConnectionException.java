package de.fraunhofer.iosb.edc.remote;

import org.eclipse.edc.spi.EdcException;

public class ControlPlaneConnectionException extends EdcException {
    public ControlPlaneConnectionException(String message) {
        super(message);
    }

    public ControlPlaneConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ControlPlaneConnectionException(Throwable cause) {
        super(cause);
    }

    public ControlPlaneConnectionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
