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
package de.fraunhofer.iosb.ilt.dataspace.model.context;

import de.fraunhofer.iosb.ilt.dataspace.aas.lib.auth.impl.ApiKey;
import de.fraunhofer.iosb.ilt.dataspace.aas.lib.auth.impl.BasicAuth;
import de.fraunhofer.iosb.ilt.dataspace.aas.lib.auth.impl.NoAuth;
import de.fraunhofer.iosb.ilt.dataspace.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.ilt.dataspace.aas.test.defaults.DefaultVault;
import de.fraunhofer.iosb.ilt.dataspace.model.context.registry.AasRegistryContext;
import de.fraunhofer.iosb.ilt.dataspace.model.context.repository.remote.RemoteAasRepositoryContext;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.util.AasUtils;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


class AasServerContextTest {

    private static final URI SERVER_URI = URI.create("http://aas.example.local");
    private final Vault vault = new DefaultVault();


    // --- getters ---


    @Test
    void getUri_returnsConfiguredUri() {
        var context = new RemoteAasRepositoryContext.Builder().uri(SERVER_URI).build();
        assertEquals(SERVER_URI, context.getUri());
    }


    @Test
    void getPolicyBindings_returnsConfiguredBindings() {
        Submodel submodel = new DefaultSubmodel.Builder().id("test-sm").idShort("test-sm").build();
        Reference reference = AasUtils.toReference(submodel);
        var bindings = List.of(PolicyBinding.ofDefaults(reference));
        var context = new RemoteAasRepositoryContext.Builder()
                .uri(SERVER_URI)
                .policyBindings(bindings)
                .build();

        assertEquals(bindings, context.getPolicyBindings());
    }


    @Test
    void isOnlySubmodels_returnsConfiguredValue() {
        var context = new RemoteAasRepositoryContext.Builder()
                .uri(SERVER_URI)
                .onlySubmodels(true)
                .build();

        assertTrue(context.isOnlySubmodels());
    }


    @Test
    void getAuthenticationMethod_returnsConfiguredMethod() {
        var auth = new ApiKey("key", "value", vault);
        var context = new RemoteAasRepositoryContext.Builder()
                .uri(SERVER_URI)
                .authenticationMethod(auth)
                .build();

        assertEquals(auth, context.getAuthenticationMethod());
    }


    @Test
    void allowSelfSigned_returnsConfiguredValue() {
        var context = new RemoteAasRepositoryContext.Builder()
                .uri(SERVER_URI)
                .allowSelfSigned(true)
                .build();

        assertTrue(context.allowSelfSigned());
    }


    // --- default values ---


    @Test
    void defaults_noAuthAndNoFlagsSet() {
        var context = new RemoteAasRepositoryContext.Builder().uri(SERVER_URI).build();

        assertInstanceOf(NoAuth.class, context.getAuthenticationMethod());
        assertFalse(context.isOnlySubmodels());
        assertFalse(context.allowSelfSigned());
        assertTrue(context.getPolicyBindings().isEmpty());
    }


    // --- requiresAuthentication ---


    @Test
    void requiresAuthentication_noAuth_returnsFalse() {
        var context = new RemoteAasRepositoryContext.Builder()
                .uri(SERVER_URI)
                .authenticationMethod(new NoAuth())
                .build();

        assertFalse(context.requiresAuthentication());
    }


    @Test
    void requiresAuthentication_default_returnsFalse() {
        var context = new RemoteAasRepositoryContext.Builder().uri(SERVER_URI).build();

        assertFalse(context.requiresAuthentication());
    }


    @Test
    void requiresAuthentication_apiKey_returnsTrue() {
        var context = new RemoteAasRepositoryContext.Builder()
                .uri(SERVER_URI)
                .authenticationMethod(new ApiKey("key", "value", vault))
                .build();

        assertTrue(context.requiresAuthentication());
    }


    @Test
    void requiresAuthentication_basicAuth_returnsTrue() {
        var context = new RemoteAasRepositoryContext.Builder()
                .uri(SERVER_URI)
                .authenticationMethod(new BasicAuth("user", "pass", vault))
                .build();

        assertTrue(context.requiresAuthentication());
    }


    // --- builder validation ---


    @Test
    void builder_nullUri_throwsNpe() {
        assertThrows(NullPointerException.class, () -> new RemoteAasRepositoryContext.Builder().build());
    }


    @Test
    void registryBuilder_nullUri_throwsNpe() {
        assertThrows(NullPointerException.class, () -> new AasRegistryContext.Builder().build());
    }


    // --- RemoteClientContext implementation ---


    @Test
    void remoteAasRepositoryContext_implementsRemoteClientContext() {
        var context = new RemoteAasRepositoryContext.Builder().uri(SERVER_URI).build();

        assertInstanceOf(RemoteClientContext.class, context);
        assertEquals(SERVER_URI, ((RemoteClientContext) context).getUri());
        assertNotNull(((RemoteClientContext) context).getAuthenticationMethod());
        assertFalse(((RemoteClientContext) context).allowSelfSigned());
    }


    @Test
    void aasRegistryContext_implementsRemoteClientContext() {
        var context = new AasRegistryContext.Builder().uri(SERVER_URI).build();

        assertInstanceOf(RemoteClientContext.class, context);
        assertEquals(SERVER_URI, ((RemoteClientContext) context).getUri());
        assertNotNull(((RemoteClientContext) context).getAuthenticationMethod());
        assertFalse(((RemoteClientContext) context).allowSelfSigned());
    }


    @Test
    void aasRegistryContext_customSettings_reflectValues() {
        var auth = new BasicAuth("user", "pass", vault);
        var context = new AasRegistryContext.Builder()
                .uri(SERVER_URI)
                .authenticationMethod(auth)
                .allowSelfSigned(true)
                .onlySubmodels(true)
                .build();

        assertTrue(context.requiresAuthentication());
        assertTrue(context.allowSelfSigned());
        assertTrue(context.isOnlySubmodels());
        assertEquals(auth, context.getAuthenticationMethod());
    }
}
