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
package de.fraunhofer.iosb.edc.remote.stores;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.edc.remote.ControlPlaneConnection;
import de.fraunhofer.iosb.edc.remote.HttpMethod;
import de.fraunhofer.iosb.edc.remote.transform.Codec;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class ControlPlaneConnectionHandler {

    public static final String MANAGEMENT = "management";
    public static final String STABLE = "stable";
    public static final String SEMVER_TAG_SEPARATOR = "-";
    public static final String UNEXPECTED_ERROR = "Unexpected error from (remote) control-plane, %s: %s";
    public static final String MALFORMED_ERROR = "MalformedRequestError from (remote) control-plane, %s: %s";

    protected final ControlPlaneConnection controlPlane;
    protected final EdcHttpClient httpClient;
    protected final Monitor monitor;
    protected final Codec codec;

    public ControlPlaneConnectionHandler(EdcHttpClient httpClient, Monitor monitor, String protocol, String hostname, int managementPort,
                                         String managementPath, ControlPlaneConnection versionConnection, Codec codec, String resourceName) throws IOException {
        this.codec = codec;
        this.httpClient = httpClient;
        this.monitor = monitor;

        // Use version endpoint to get latest (stable) management API endpoint
        String managementVersion = getManagementEndpoint(versionConnection);
        this.controlPlane = new ControlPlaneConnection(protocol, hostname, managementPort,
                managementPath + managementVersion + "/" + resourceName);
    }

    public ControlPlaneConnectionHandler(EdcHttpClient httpClient, Monitor monitor, String managementUrl, Codec codec, String resourceName) throws MalformedURLException {
        this.codec = codec;
        this.httpClient = httpClient;
        this.monitor = monitor;

        // Use version endpoint to get latest (stable) management API endpoint
        this.controlPlane = new ControlPlaneConnection(managementUrl);
    }

    protected ServiceResult<String> executeRequest(Request request) {
        try (Response response = this.httpClient.execute(request)) {

            ResponseBody body = response.body();
            if (!response.isSuccessful()) {
                // User error: 404, 409
                return switch (response.code()) {
                    case 404 -> ServiceResult.notFound(response.body().string());
                    case 409 -> ServiceResult.conflict(response.body().string());
                    case 400 -> throw new EdcException(String.format(MALFORMED_ERROR, response.message(), response.body().string()));
                    default -> throw new EdcException(String.format(UNEXPECTED_ERROR, response.message(), response.body().string()));
                };
            }
            return ServiceResult.success(body.string());

        } catch (IOException controlPlaneConnectionException) {
            return ServiceResult.unexpected(controlPlaneConnectionException.getMessage());
        }
    }


    private String getManagementEndpoint(ControlPlaneConnection versionConnection) throws IOException {
        monitor.info(String.format("Getting management endpoint version from remote control plane at %s...", versionConnection.toString()));
        var request = versionConnection.prepareRequest(HttpMethod.GET);

        String versionResponseBody;
        try (Response versionResponse = httpClient.execute(request)) {
            ResponseBody body = versionResponse.body();
            if (versionResponse.isSuccessful()) {
                versionResponseBody = body.string();
            } else {
                throw new EdcException(String.format("Could not get /version response from control plane: %s", versionResponse.message()));
            }
        }

        TypeReference<HashMap<String, List<VersionRecord>>> typeRef = new TypeReference<>() {
        };
        // ObjectMapper is only used once during object creation phase
        Map<String, List<VersionRecord>> versionResponseEndpoints = new ObjectMapper().readValue(versionResponseBody, typeRef);
        List<VersionRecord> managementEndpoints = versionResponseEndpoints.get(MANAGEMENT);

        return managementEndpoints.stream().filter(versionRecord -> versionRecord.maturity().equals(STABLE)).min(ControlPlaneConnectionHandler::compareVersionRecordsByVersion).map(VersionRecord::urlPath).orElseThrow(() -> new EdcException("Failed getting most current management API version from control-plane."));
    }


    private static int compareVersionRecordsByVersion(VersionRecord recordA, VersionRecord recordB) {
        var aVersions = recordA.version().split("\\.");
        var bVersions = recordB.version().split("\\.");

        for (int i = 0; i < aVersions.length; i++) {
            int aVersion;
            int bVersion;
            try {
                // Warning: If two STABLE versions with the same numbers exist but with different tags, they are treated as equal
                aVersion = Integer.parseInt(aVersions[i].split(SEMVER_TAG_SEPARATOR)[0]);
                bVersion = Integer.parseInt(bVersions[i].split(SEMVER_TAG_SEPARATOR)[0]);
            } catch (NumberFormatException versionParsingException) {
                throw new EdcException("Failed parsing management API version from control-plane version endpoint.", versionParsingException);
            }

            if (aVersion != bVersion) {
                return aVersion - bVersion;
            }

            boolean hasTagA = aVersions[i].split("-").length > 1;
            boolean hasTagB = bVersions[i].split("-").length > 1;
            if (hasTagA && hasTagB || !hasTagA && !hasTagB) {
                continue;
            }
            return hasTagA ? 1 : -1;
        }
        return 0;
    }

    // Avoid java.time.Instant as a type because of problems with the ObjectMapper
    private record VersionRecord(String version, String urlPath, String lastUpdated, String maturity) {
    }

    public static abstract class Builder<T extends ControlPlaneConnectionHandler, B extends Builder<T, B>> {
        protected EdcHttpClient httpClient;
        protected Monitor monitor;
        protected String protocol = "http";
        protected String hostname = "localhost";
        protected int managementPort = 8281;
        protected String managementPath = "/management/";
        protected int versionPort = 7171;
        protected String versionPath = "/version";
        protected TypeTransformerRegistry typeTransformerRegistry;
        protected TypeManager typeManager;
        protected JsonLd jsonLd;
        protected String fullManagementUrl;

        protected abstract B self();

        protected abstract T create(EdcHttpClient httpClient, Monitor monitor,
                                    String protocol, String hostname, int managementPort, String managementPath,
                                    ControlPlaneConnection versionConnection,
                                    Codec codec) throws IOException;

        protected abstract T create(EdcHttpClient httpClient, Monitor monitor, String fullManagementUrl, Codec codec) throws MalformedURLException;

        public B httpClient(EdcHttpClient v) {
            this.httpClient = v;
            return self();
        }

        public B monitor(Monitor v) {
            this.monitor = v;
            return self();
        }

        public B protocol(String v) {
            this.protocol = v;
            return self();
        }

        public B hostname(String v) {
            this.hostname = v;
            return self();
        }

        public B managementPort(int v) {
            this.managementPort = v;
            return self();
        }

        public B managementPath(String v) {
            this.managementPath = v;
            return self();
        }

        public B versionPort(int v) {
            this.versionPort = v;
            return self();
        }

        public B versionPath(String v) {
            this.versionPath = v;
            return self();
        }

        public B typeTransformerRegistry(TypeTransformerRegistry v) {
            this.typeTransformerRegistry = v;
            return self();
        }

        public B typeManager(TypeManager v) {
            this.typeManager = v;
            return self();
        }

        public B jsonLd(JsonLd v) {
            this.jsonLd = v;
            return self();
        }

        public B fullManagementUrl(String path) {
            this.fullManagementUrl = path;
            return self();
        }

        public T build() {
            Objects.requireNonNull(httpClient, "httpClient must not be null");
            Objects.requireNonNull(monitor, "monitor must not be null");
            Objects.requireNonNull(typeTransformerRegistry, "typeTransformerRegistry must not be null");
            Objects.requireNonNull(typeManager, "typeManager must not be null");
            Objects.requireNonNull(jsonLd, "jsonLd must not be null");

            var codec = new Codec(typeTransformerRegistry, typeManager, jsonLd);

            if (fullManagementUrl != null) {
                try {
                    return create(httpClient, monitor, fullManagementUrl, codec);
                } catch (MalformedURLException managementUrlMalformedException) {
                    throw new EdcException("Failed to build ControlPlaneConnectionHandler", managementUrlMalformedException);
                }
            }

            var versionConnection = new ControlPlaneConnection(protocol, hostname, versionPort, versionPath);

            try {
                return create(httpClient, monitor, protocol, hostname, managementPort, managementPath,
                        versionConnection, codec);
            } catch (IOException controlPlaneConnectionException) {
                throw new EdcException("Failed to establish control plane connection", controlPlaneConnectionException);
            }
        }
    }
}
