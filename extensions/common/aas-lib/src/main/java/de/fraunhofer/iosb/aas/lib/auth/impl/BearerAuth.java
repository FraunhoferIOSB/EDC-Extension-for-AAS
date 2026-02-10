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
package de.fraunhofer.iosb.aas.lib.auth.impl;

import de.fraunhofer.iosb.aas.lib.auth.AuthenticationMethod;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2CredentialsRequest;
import org.eclipse.edc.iam.oauth2.spi.client.SharedSecretOauth2CredentialsRequest;
import org.eclipse.edc.spi.security.Vault;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.function.Function;
import javax.naming.OperationNotSupportedException;


public class BearerAuth extends AuthenticationMethod {
    private final Function<Vault, String> clientId;
    private final Function<Vault, String> clientSecret;

    private final Function<Vault, String> username;
    private final Function<Vault, String> password;

    private final URI identityProvider;
    private final Oauth2Client client;


    public BearerAuth(String clientId, String clientSecret, String username, String password, URI identityProvider, Oauth2Client client, Vault vault) {
        this.identityProvider = identityProvider;
        this.client = client;
        this.clientId = getResolver(vault, clientId);
        this.clientSecret = getResolver(vault, clientSecret);
        this.username = getResolver(vault, username);
        this.password = getResolver(vault, password);
    }


    @Override
    public String getValue(Vault vault) {
        Oauth2CredentialsRequest req = SharedSecretOauth2CredentialsRequest.Builder.newInstance()
                .url(identityProvider.toString())
                .grantType("client_credentials")
                .clientId(clientId.apply(vault))
                .clientSecret(clientSecret.apply(vault))
                .param("username", username.apply(vault))
                .param("password", password.apply(vault))
                .build();
        String token = client.requestToken(req)
                .orElseThrow((failure) -> new RuntimeException(failure.getFailureDetail()))
                .getToken();

        return "Bearer ".concat(token);
    }


    @Override
    public HttpClient.Builder httpClientBuilderFor(Vault vault) {
        throw new RuntimeException(new OperationNotSupportedException("Authorization headers cannot be registered directly at the http client."));
    }
}
