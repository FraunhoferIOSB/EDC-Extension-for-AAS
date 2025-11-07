package de.fraunhofer.iosb.client.registry;

import de.fraunhofer.iosb.aas.lib.util.InetTools;
import de.fraunhofer.iosb.client.AasServerClient;
import de.fraunhofer.iosb.client.exception.UnauthorizedException;
import de.fraunhofer.iosb.ilt.faaast.client.exception.ConnectivityException;
import de.fraunhofer.iosb.ilt.faaast.client.exception.ForbiddenException;
import de.fraunhofer.iosb.ilt.faaast.client.exception.MethodNotAllowedException;
import de.fraunhofer.iosb.ilt.faaast.client.exception.StatusCodeException;
import de.fraunhofer.iosb.ilt.faaast.client.interfaces.AASRegistryInterface;
import de.fraunhofer.iosb.ilt.faaast.client.interfaces.SubmodelRegistryInterface;
import de.fraunhofer.iosb.model.context.registry.AasRegistryContext;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShellDescriptor;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodelDescriptor;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;

public class AasRegistryClient implements AasServerClient {

    // FA³ST client
    private final AASRegistryInterface aasRegistryInterface;
    private final SubmodelRegistryInterface submodelRegistryInterface;
    private final AasRegistryContext context;

    public AasRegistryClient(AasRegistryContext context) {
        this.context = context;
        HttpClient httpClient = context.getAuthenticationMethod().httpClientBuilderFor().build();
        // TODO when client gets builder, revise this
        this.aasRegistryInterface = new AASRegistryInterface(context.getUri(), true);
        this.submodelRegistryInterface = new SubmodelRegistryInterface(context.getUri(), true);
    }

    @Override
    public boolean isAvailable() {
        return InetTools.pingHost(context.getUri().getHost(), context.getUri().getPort());
    }

    /**
     * Get all AAS descriptors published by the registry.
     *
     * @return List of AAS descriptors as published by the registry.
     * @throws UnauthorizedException A call to this registry was unauthorized.
     * @throws ConnectException      A call to this registry was not possible due to a connection issue.
     */
    public List<DefaultAssetAdministrationShellDescriptor> getShellDescriptors() throws UnauthorizedException, ConnectException {
        try {
            return aasRegistryInterface.getAll();
        } catch (ForbiddenException | de.fraunhofer.iosb.ilt.faaast.client.exception.UnauthorizedException |
                 MethodNotAllowedException unauthorizedException) {
            throw new UnauthorizedException(unauthorizedException);
        } catch (ConnectivityException | de.fraunhofer.iosb.ilt.faaast.client.exception.NotFoundException e) {
            throw new ConnectException(e.getMessage());
        } catch (StatusCodeException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get all submodel descriptors published by the registry.
     *
     * @return List of submodel descriptors as published by the registry.
     * @throws UnauthorizedException A call to this registry was unauthorized.
     * @throws ConnectException      A call to this registry was not possible due to a connection issue.
     */
    public List<DefaultSubmodelDescriptor> getSubmodelDescriptors() throws UnauthorizedException, ConnectException {
        try {
            return submodelRegistryInterface.getAll();
        } catch (ForbiddenException | de.fraunhofer.iosb.ilt.faaast.client.exception.UnauthorizedException |
                 MethodNotAllowedException unauthorizedException) {
            throw new UnauthorizedException(unauthorizedException);
        } catch (ConnectivityException e) {
            throw new ConnectException(e.getMessage());
        } catch (StatusCodeException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public URI getUri() {
        return context.getUri();
    }
}
