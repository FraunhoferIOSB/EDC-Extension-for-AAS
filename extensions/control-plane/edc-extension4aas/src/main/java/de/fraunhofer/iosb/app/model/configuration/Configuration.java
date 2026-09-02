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
package de.fraunhofer.iosb.app.model.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static de.fraunhofer.iosb.constants.AasConstants.DEFAULT_EXPOSED_FIELDS;


/**
 * The configuration of the application.
 */
public class Configuration {

    /** Default constructor. */
    public Configuration() {}


    private static final String SETTINGS_PREFIX = "edc.aas.";
    private static Configuration instance;
    @JsonProperty(SETTINGS_PREFIX + "syncPeriod")
    private int syncPeriod = 50; // Seconds
    @JsonProperty(SETTINGS_PREFIX + "onlySubmodels")
    private boolean onlySubmodels = true;
    @JsonProperty(SETTINGS_PREFIX + "exposeSelfDescription")
    private boolean exposeSelfDescription = true;
    @JsonProperty(SETTINGS_PREFIX + "allowSelfSignedCertificates")
    private boolean allowSelfSignedCertificates;
    @JsonProperty(SETTINGS_PREFIX + "remoteAASLocation")
    private URI remoteAasLocation;
    @JsonProperty(SETTINGS_PREFIX + "localAASModelPath")
    private String localAasModelPath;
    @JsonProperty(SETTINGS_PREFIX + "localAASServicePort")
    private Integer localAasServicePort;
    @JsonProperty(SETTINGS_PREFIX + "localAASServiceConfigPath")
    private String aasServiceConfigPath;
    @JsonProperty(SETTINGS_PREFIX + "defaultAccessPolicyPath")
    private String defaultAccessPolicyPath;
    @JsonProperty(SETTINGS_PREFIX + "defaultContractPolicyPath")
    private String defaultContractPolicyPath;
    @JsonProperty(SETTINGS_PREFIX + "useAasDataPlane")
    private boolean useAasDataPlane = true;
    @JsonProperty(SETTINGS_PREFIX + "exposedFields")
    private Set<String> exposedFields;
    @JsonProperty(SETTINGS_PREFIX + "hercules.enabled")
    private boolean hercules;


    /**
     * Returns the singleton configuration instance, creating it lazily if needed.
     *
     * @return The singleton configuration instance.
     */
    public static synchronized Configuration getInstance() {
        if (instance == null) {
            instance = new Configuration();
        }
        return instance;
    }


    /**
     * Returns the remote AAS location.
     *
     * @return The remote AAS location.
     */
    public URI getRemoteAasLocation() {
        return remoteAasLocation;
    }


    /**
     * Returns the local AAS model path.
     *
     * @return The local AAS model path.
     */
    public String getLocalAasModelPath() {
        return localAasModelPath;
    }


    /**
     * Returns the local AAS service port.
     *
     * @return The local AAS service port.
     */
    public Integer getLocalAasServicePort() {
        return localAasServicePort;
    }


    /**
     * Returns the AAS service configuration path.
     *
     * @return The AAS service configuration path.
     */
    public String getAasServiceConfigPath() {
        return aasServiceConfigPath;
    }


    /**
     * Returns the sync period in seconds.
     *
     * @return The sync period in seconds.
     */
    public int getSyncPeriod() {
        return syncPeriod;
    }


    /**
     * Returns whether only submodels should be registered.
     *
     * @return Whether only submodels should be registered.
     */
    public boolean onlySubmodels() {
        return onlySubmodels;
    }


    /**
     * Returns whether the self-description should be exposed.
     *
     * @return Whether the self-description should be exposed.
     */
    public boolean isExposeSelfDescription() {
        return exposeSelfDescription;
    }


    /**
     * Returns the default access policy path.
     *
     * @return The default access policy path.
     */
    public String getDefaultAccessPolicyPath() {
        return defaultAccessPolicyPath;
    }


    /**
     * Returns the default contract policy path.
     *
     * @return The default contract policy path.
     */
    public String getDefaultContractPolicyPath() {
        return defaultContractPolicyPath;
    }


    /**
     * Returns whether self-signed certificates are allowed.
     *
     * @return Whether self-signed certificates are allowed.
     */
    public boolean isAllowSelfSignedCertificates() {
        return allowSelfSignedCertificates;
    }


    /**
     * Returns whether the AAS data plane should be used.
     *
     * @return Whether the AAS data plane should be used.
     */
    public boolean useAasDataPlane() {
        return useAasDataPlane;
    }


    /**
     * Returns the exposed fields.
     *
     * @return The exposed fields.
     */
    public Set<String> getExposedFields() {
        return exposedFields;
    }


    /**
     * Sets the exposed fields from a comma-separated string. If the value is {@code null}, the default exposed fields
     * are used.
     *
     * @param exposedFields Comma-separated list of exposed fields.
     */
    public void setExposedFields(String exposedFields) {
        Optional.ofNullable(exposedFields)
                .ifPresentOrElse(
                        ef -> this.exposedFields = Arrays.stream(ef.split(",")).map(String::trim)
                                .collect(Collectors.toSet()),
                        () -> this.exposedFields = DEFAULT_EXPOSED_FIELDS);
    }


    /**
     * Returns whether Hercules mode is enabled.
     *
     * @return Whether Hercules mode is enabled.
     */
    public boolean isHercules() {
        return hercules;
    }
}
