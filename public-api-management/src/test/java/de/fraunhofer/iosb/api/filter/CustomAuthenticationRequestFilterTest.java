/*
 * Copyright (c) 2021 Fraunhofer IOSB, eine rechtlich nicht selbstaendige
 * Einrichtung der Fraunhofer-Gesellschaft zur Foerderung der angewandten
 * Forschung e.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fraunhofer.iosb.api.filter;

import de.fraunhofer.iosb.api.model.Endpoint;
import de.fraunhofer.iosb.api.model.HttpMethod;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.edc.api.auth.spi.AuthenticationService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static de.fraunhofer.iosb.api.model.EndpointTest.createNormalEndpoint;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomAuthenticationRequestFilterTest {

    CustomAuthenticationRequestFilter customAuthenticationRequestFilter;
    AuthenticationService mockAuthenticationService;

    @BeforeEach
    void setUp() {
        mockAuthenticationService = mock(AuthenticationService.class);
        customAuthenticationRequestFilter = new CustomAuthenticationRequestFilter(mockAuthenticationService, mock(Monitor.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void filterGoThrough() {
        // Make a request that should be accepted by our filter.
        // First, prepare the filter by adding an endpoint.
        var headers = Map.of("y-api-key", List.of("pasword"));
        customAuthenticationRequestFilter.addEndpoints(List.of(new Endpoint("/api/suffix/test", HttpMethod.DELETE, headers)));
        // Create the mock request
        var mockRequest = mock(ContainerRequestContext.class);
        var mockUriInfo = mock(UriInfo.class);
        when(mockUriInfo.getPath()).thenReturn("/api/suffix/test");
        when(mockRequest.getUriInfo()).thenReturn(mockUriInfo);
        when(mockRequest.getMethod()).thenReturn("DELETE");
        var mockMultiValueMap = mock(MultivaluedMap.class);
        when(mockMultiValueMap.entrySet()).thenReturn(headers.entrySet());
        when(mockRequest.getHeaders()).thenReturn(mockMultiValueMap);

        // This should not be called
        when(mockAuthenticationService.isAuthenticated(any())).thenThrow(IllegalAccessError.class);

        // This should run through
        customAuthenticationRequestFilter.filter(mockRequest);
    }

    @SuppressWarnings("unchecked")
    @Test
    void filterWrongHttpMethod() {
        // Make a request that should not be accepted by our filter.
        // First, prepare the filter by adding an endpoint.
        var headers = Map.of("y-api-key", List.of("pasword"));
        customAuthenticationRequestFilter.addEndpoints(List.of(new Endpoint("/api/suffix/test", HttpMethod.PATCH, headers)));
        // Create the mock request
        var mockRequest = mock(ContainerRequestContext.class);
        var mockUriInfo = mock(UriInfo.class);
        when(mockUriInfo.getPath()).thenReturn("/api/suffix/test");
        when(mockRequest.getUriInfo()).thenReturn(mockUriInfo);
        when(mockRequest.getMethod()).thenReturn("DELETE");
        var mockMultiValueMap = mock(MultivaluedMap.class);
        when(mockMultiValueMap.entrySet()).thenReturn(headers.entrySet());
        when(mockRequest.getHeaders()).thenReturn(mockMultiValueMap);


        try {
            // This should delegate request to superclass
            customAuthenticationRequestFilter.filter(mockRequest);
            fail();
        } catch (AuthenticationFailedException expected) {
        }
        // This should be called once
        verify(mockAuthenticationService, times(1)).isAuthenticated(any());
    }

    @Test
    void addEndpoints() {
        // Pre: No endpoints
        // We only test if adding breaks
        var endpoints = new ArrayList<>(List.of(createNormalEndpoint(), createNormalEndpoint(), createNormalEndpoint()));
        assertTrue(customAuthenticationRequestFilter.addEndpoints(endpoints));
    }

    @SuppressWarnings("unchecked")
    @Test
    void addSameEndpointsUnchanged() {
        var endpoints = new ArrayList<>(List.of(createNormalEndpoint(), createNormalEndpoint(), createNormalEndpoint()));
        customAuthenticationRequestFilter.addEndpoints(endpoints);

        // Pre: Three endpoints
        // Test if adding the same endpoint(s) changes the state

        ArrayList<Endpoint> sameEndpoints = (ArrayList<Endpoint>) endpoints.clone();
        assertFalse(customAuthenticationRequestFilter.addEndpoints(sameEndpoints));
    }

    @Test
    void removeEndpoints() {
        var endpoints = new ArrayList<>(List.of(createNormalEndpoint(), createNormalEndpoint(), createNormalEndpoint()));
        customAuthenticationRequestFilter.addEndpoints(endpoints);

        // Pre: Three endpoints
        // Test if removing these same endpoints changes the state
        var sameEndpoints = cloneEndpoints(endpoints);
        assertTrue(customAuthenticationRequestFilter.removeEndpoints(sameEndpoints));

        // Now all endpoints should be removed -> removing again should not change state
        sameEndpoints = cloneEndpoints(endpoints);
        assertFalse(customAuthenticationRequestFilter.removeEndpoints(sameEndpoints));
    }

    private ArrayList<Endpoint> cloneEndpoints(ArrayList<Endpoint> endpointCollection) {
        var sameEndpoints = new ArrayList<>(endpointCollection);
        sameEndpoints.forEach(oldEndpoint -> new Endpoint(oldEndpoint.suffix(), oldEndpoint.method(), oldEndpoint.customHeaders()));
        return sameEndpoints;
    }
}