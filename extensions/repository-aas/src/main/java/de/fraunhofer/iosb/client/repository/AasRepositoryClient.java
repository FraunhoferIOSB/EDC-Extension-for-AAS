package de.fraunhofer.iosb.client.repository;

import de.fraunhofer.iosb.client.AasServerClient;
import de.fraunhofer.iosb.client.exception.UnauthorizedException;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;

import java.net.ConnectException;


public interface AasRepositoryClient extends AasServerClient {
    Environment getEnvironment() throws ConnectException, UnauthorizedException;
}
