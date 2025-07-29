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
package de.fraunhofer.iosb.ilt.api;

import de.fraunhofer.iosb.ilt.api.filter.CustomAuthenticationRequestFilter;
import de.fraunhofer.iosb.ilt.api.model.Endpoint;
import de.fraunhofer.iosb.ilt.api.model.HttpMethod;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.edc.api.auth.spi.registry.ApiAuthenticationRegistry;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicApiManagementServiceTest {

    private final Monitor monitor = new ConsoleMonitor().withPrefix("PublicApiManagementServiceTest");

    private PublicApiManagementService testSubject;

    private CustomAuthenticationRequestFilter customAuthenticationRequestFilterSpy;

    private ApiAuthenticationRegistry apiAuthenticationRegistry;

    @BeforeEach
    void setUp() {
        apiAuthenticationRegistry = mock(ApiAuthenticationRegistry.class);

        customAuthenticationRequestFilterSpy = spy(new CustomAuthenticationRequestFilter(apiAuthenticationRegistry,
                monitor));

        testSubject = new PublicApiManagementService(customAuthenticationRequestFilterSpy, monitor);
    }

    @Test
    void test_addEndpoints_normalBehaviourOneEndpointAdded() {
        var endpoint = createEndpoint();

        testSubject.addEndpoints(Collections.singleton(endpoint));

        verifyNormalOperationOnPublicEndpoint(endpoint);
    }


    @Test
    void test_addEndpoints_normalBehaviourMultipleEndpointAdded() {
        var verificationEndpoint = createEndpoint("/test", HttpMethod.PATCH, Map.of("hello", List.of("world")));
        var endpoints = List.of(createEndpoint(), verificationEndpoint);

        testSubject.addEndpoints(endpoints);

        verifyNormalOperationOnPublicEndpoint(verificationEndpoint);
    }

    private void verifyNormalOperationOnPublicEndpoint(Endpoint endpoint) {
        try {
            // If this operation fails, the endpoint was not added (correctly)
            customAuthenticationRequestFilterSpy.filter(mockContainerRequestContext(endpoint));
        } catch (AuthenticationFailedException endpointNotAddedCorrectlyException) {
            fail("Endpoint was not added (correctly)");
        }

        // Resolve operation happens only if a called endpoint is not registered at PublicApiManagementService
        // We cannot test if customAuthRequestFilter.super.filter() is called...
        verify(apiAuthenticationRegistry, never()).resolve(any());

        verify(customAuthenticationRequestFilterSpy, times(1)).addEndpoints(any());
    }

    private Endpoint createEndpoint(String path, HttpMethod method, Map<String, List<String>> headers) {
        return new Endpoint(path, method, headers);
    }

    private Endpoint createEndpoint() {
        return createEndpoint("/method", HttpMethod.DELETE,
                Map.of("api-key", List.of("testing", "beep", "boop")));
    }

    /* For sending verification requests to the customAuthFilter */
    private ContainerRequestContext mockContainerRequestContext(Endpoint fromEndpoint) {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putAll(fromEndpoint.customHeaders());

        var context = mock(ContainerRequestContext.class);
        var uriInfo = mock(UriInfo.class);
        when(uriInfo.getPath()).thenReturn(fromEndpoint.suffix());
        when(context.getUriInfo()).thenReturn(uriInfo);

        when(context.getMethod()).thenReturn(fromEndpoint.method().toString());
        when(context.getHeaders()).thenReturn(headers);

        return context;
    }

}