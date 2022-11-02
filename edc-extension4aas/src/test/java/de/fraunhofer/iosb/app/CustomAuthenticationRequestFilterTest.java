package de.fraunhofer.iosb.app;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.dataspaceconnector.api.auth.AuthenticationService;
import org.eclipse.dataspaceconnector.spi.exception.AuthenticationFailedException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.fraunhofer.iosb.app.model.configuration.Configuration;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;

public class CustomAuthenticationRequestFilterTest {

    AuthenticationService authService = mock(AuthenticationService.class);
    CustomAuthenticationRequestFilter authRequestFilter = new CustomAuthenticationRequestFilter(authService);

    @BeforeAll
    public static void initializeLogger() {
        Logger.getInstance().setMonitor(mock(Monitor.class));
    }

    @Test
    void filterSelfDescriptionTest() {
        verify(authService, times(0)).isAuthenticated(any());
        setupConfigExposeSelfDescriptionValue(true);
        var mockedContext = createSemiAuthenticRequestContext(Endpoint.SELF_DESCRIPTION_PATH, false);
        authRequestFilter.filter(mockedContext);
    }

    @Test
    void filterRequestUnauthenticatedTest() {
        setupConfigExposeSelfDescriptionValue(true);
        var mockedContext = createSemiAuthenticRequestContext("config", false);

        try {
            authRequestFilter.filter(mockedContext);
            fail();
        } catch (AuthenticationFailedException expected) {
        }
    }

    @Test
    void filterRequestAuthenticatedTest() {
        setupConfigExposeSelfDescriptionValue(true);
        var mockedContext = createSemiAuthenticRequestContext("unauthorizedPath", true);
        authRequestFilter.filter(mockedContext);
    }

    private void setupConfigExposeSelfDescriptionValue(boolean expose) {
        Configuration conf = Configuration.getInstance();
        conf.setExposeSelfDescription(expose);
    }

    /*
     * Just enough parameters are mocked so that the super class filter method does
     * not crash
     */
    private ContainerRequestContext createSemiAuthenticRequestContext(String returnedPath,
            boolean isAuthenticatedMockResponse) {
        ContainerRequestContext mockedContainerRequestContext = mock(ContainerRequestContext.class);
        UriInfo mockedUriInfo = mock(UriInfo.class);
        when(mockedUriInfo.getPath()).thenReturn(returnedPath);

        when(mockedContainerRequestContext.getUriInfo()).thenReturn(mockedUriInfo);

        // Super class needs these to not crash
        when(mockedContainerRequestContext.getHeaders()).thenReturn(new MultivaluedHashMap<String, String>());
        when(mockedContainerRequestContext.getMethod()).thenReturn("POST");
        setAuthenticatedBySuperclass(isAuthenticatedMockResponse);
        return mockedContainerRequestContext;
    }

    private void setAuthenticatedBySuperclass(boolean authenticated) {
        when(authService.isAuthenticated(any())).thenReturn(authenticated);
    }
}
