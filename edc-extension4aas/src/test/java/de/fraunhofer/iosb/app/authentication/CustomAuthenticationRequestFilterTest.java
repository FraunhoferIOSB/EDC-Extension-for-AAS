package de.fraunhofer.iosb.app.authentication;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Objects;

import org.eclipse.edc.api.auth.spi.AuthenticationService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.fraunhofer.iosb.app.Endpoint;
import de.fraunhofer.iosb.app.Logger;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;

public class CustomAuthenticationRequestFilterTest {

    AuthenticationService authService = mock(AuthenticationService.class);
    CustomAuthenticationRequestFilter authRequestFilter;

    @BeforeAll
    public static void initializeLogger() {
        Logger.getInstance().setMonitor(mock(Monitor.class));
    }

    @BeforeEach
    public void initializeTestObject() {
        authRequestFilter = new CustomAuthenticationRequestFilter(authService, "selfDescription");
    }

    void filterDataTransferTest() {
        authRequestFilter.addTemporaryApiKey("test-key", "test-password");

        verify(authService, times(0)).isAuthenticated(any());

        var mockedContext = createSemiAuthenticRequestContext(Endpoint.SELF_DESCRIPTION_PATH, false,
                new MultivaluedHashMap<>(Map.of("test-key", "test-passord")));
        authRequestFilter.filter(mockedContext);

    }

    @Test
    void filterSelfDescriptionTest() {
        verify(authService, times(0)).isAuthenticated(any());

        var mockedContext = createSemiAuthenticRequestContext(Endpoint.SELF_DESCRIPTION_PATH, false);
        authRequestFilter.filter(mockedContext);
    }

    @Test
    void filterRequestUnauthenticatedTest() {
        var mockedContext = createSemiAuthenticRequestContext("config", false);

        try {
            authRequestFilter.filter(mockedContext);
            fail();
        } catch (AuthenticationFailedException expected) {
        }
    }

    @Test
    void filterRequestAuthenticatedTest() {
        var mockedContext = createSemiAuthenticRequestContext("unauthorizedPath", true);
        authRequestFilter.filter(mockedContext);
    }

    private ContainerRequestContext createSemiAuthenticRequestContext(String returnedPath,
            boolean isAuthenticatedMockResponse) {
        return createSemiAuthenticRequestContext(returnedPath, isAuthenticatedMockResponse, null);
    }

    /*
     * Just enough parameters are mocked so that the super class filter method does
     * not crash
     */
    private ContainerRequestContext createSemiAuthenticRequestContext(String returnedPath,
            boolean isAuthenticatedMockResponse, MultivaluedMap<String, String> additionalHeaders) {
        ContainerRequestContext mockedContainerRequestContext = mock(ContainerRequestContext.class);
        UriInfo mockedUriInfo = mock(UriInfo.class);
        when(mockedUriInfo.getPath()).thenReturn(returnedPath);

        when(mockedContainerRequestContext.getUriInfo()).thenReturn(mockedUriInfo);

        // Super class needs these to not crash
        when(mockedContainerRequestContext.getHeaders())
                .thenReturn(Objects.nonNull(additionalHeaders) ? additionalHeaders : new MultivaluedHashMap<>());
        when(mockedContainerRequestContext.getMethod()).thenReturn("POST");
        setAuthenticatedBySuperclass(isAuthenticatedMockResponse);
        return mockedContainerRequestContext;
    }

    private void setAuthenticatedBySuperclass(boolean authenticated) {
        when(authService.isAuthenticated(any())).thenReturn(authenticated);
    }
}
