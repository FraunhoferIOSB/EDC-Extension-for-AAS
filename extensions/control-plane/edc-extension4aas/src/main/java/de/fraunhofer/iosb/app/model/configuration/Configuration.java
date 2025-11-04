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

/**
 * The configuration of the application.
 */
public class Configuration {

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
    private int localAasServicePort;
    @JsonProperty(SETTINGS_PREFIX + "localAASServiceConfigPath")
    private String aasServiceConfigPath;
    @JsonProperty(SETTINGS_PREFIX + "defaultAccessPolicyPath")
    private String defaultAccessPolicyPath;

    @JsonProperty(SETTINGS_PREFIX + "defaultContractPolicyPath")
    private String defaultContractPolicyPath;

    @JsonProperty(SETTINGS_PREFIX + "useAasDataPlane")
    private boolean useAasDataPlane;


    public static synchronized Configuration getInstance() {
        if (instance == null) {
            instance = new Configuration();
        }
        return instance;
    }

    public URI getRemoteAasLocation() {
        return remoteAasLocation;
    }

    public String getLocalAasModelPath() {
        return localAasModelPath;
    }

    public int getLocalAasServicePort() {
        return localAasServicePort;
    }

    public String getAasServiceConfigPath() {
        return aasServiceConfigPath;
    }

    public int getSyncPeriod() {
        return syncPeriod;
    }

    public boolean onlySubmodels() {
        return onlySubmodels;
    }

    public boolean isExposeSelfDescription() {
        return exposeSelfDescription;
    }

    public String getDefaultAccessPolicyPath() {
        return defaultAccessPolicyPath;
    }

    public String getDefaultContractPolicyPath() {
        return defaultContractPolicyPath;
    }

    public boolean isAllowSelfSignedCertificates() {
        return allowSelfSignedCertificates;
    }

    public boolean useAasDataPlane() {
        return useAasDataPlane;
    }

}
