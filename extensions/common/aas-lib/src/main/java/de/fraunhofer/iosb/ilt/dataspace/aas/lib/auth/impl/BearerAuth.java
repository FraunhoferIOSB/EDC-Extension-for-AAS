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
package de.fraunhofer.iosb.ilt.dataspace.aas.lib.auth.impl;

import de.fraunhofer.iosb.ilt.dataspace.aas.lib.auth.AuthenticationMethod;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2CredentialsRequest;
import org.eclipse.edc.iam.oauth2.spi.client.SharedSecretOauth2CredentialsRequest;
import org.eclipse.edc.spi.security.Vault;

import java.net.URI;
import java.net.http.HttpClient;
import javax.naming.OperationNotSupportedException;


/** Bearer token authentication using OAuth2 client credentials. */
public class BearerAuth extends AuthenticationMethod {
    private final String clientId;
    private final String clientSecretAlias;
    private final URI identityProvider;
    private final Oauth2Client client;


    /**
     * Creates a new BearerAuth authentication.
     *
     * @param clientId the OAuth2 client ID.
     * @param clientSecret the OAuth2 client secret.
     * @param identityProvider the URI of the OAuth2 identity provider.
     * @param client the OAuth2 client used to request tokens.
     * @param vault the vault to store credentials in.
     */
    public BearerAuth(String clientId, String clientSecret, URI identityProvider, Oauth2Client client, Vault vault) {
        this.clientId = clientId;
        this.client = client;
        this.identityProvider = identityProvider;
        this.clientSecretAlias = store(vault, clientSecret);
    }


    @Override
    public String getKey() {
        return "Authorization";
    }


    public String getValue(Vault vault) {
        Oauth2CredentialsRequest req = SharedSecretOauth2CredentialsRequest.Builder.newInstance()
                .url(identityProvider.toString())
                .grantType("client_credentials")
                .clientId(clientId)
                .clientSecret(vault.resolveSecret(clientSecretAlias))
                .build();

        String token = client.requestToken(req)
                .orElseThrow((failure) -> new RuntimeException(failure.getFailureDetail()))
                .getToken();
        return String.format("Bearer %s", token);
    }


    @Override
    public void decorate(HttpDataAddress.Builder addressBuilder) {
        addressBuilder.property("oauth2:tokenUrl", identityProvider);
        addressBuilder.property("oauth2:clientId", clientId);
        addressBuilder.property("oauth2:clientSecretKey", clientSecretAlias);
    }


    @Override
    public HttpClient.Builder httpClientBuilderFor(Vault vault) {
        throw new RuntimeException(new OperationNotSupportedException("Authorization headers cannot be registered directly at the http client."));
    }
}
