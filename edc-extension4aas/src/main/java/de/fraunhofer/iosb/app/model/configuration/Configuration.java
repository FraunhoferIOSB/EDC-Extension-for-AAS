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

import java.net.URL;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Singleton class.
 * The configuration of the application.
 */
public class Configuration {

    private static final String SETTINGS_PREFIX = "edc.aas.";
    private static Configuration instance;

    @JsonProperty(SETTINGS_PREFIX + "remoteAASLocation")
    private URL remoteAasLocation;

    @JsonProperty(SETTINGS_PREFIX + "localAASModelPath")
    private String localAasModelPath;

    @JsonProperty(SETTINGS_PREFIX + "localAASServicePort")
    private int localAasServicePort;

    @JsonProperty(SETTINGS_PREFIX + "localAASServiceConfigPath")
    private String aasServiceConfigPath;

    @JsonProperty(SETTINGS_PREFIX + "syncPeriod")
    private int syncPeriod = 5; // Seconds
    
    @JsonProperty(SETTINGS_PREFIX + "onlySubmodels")
    private boolean onlySubmodels = false;

    @JsonProperty(SETTINGS_PREFIX + "exposeSelfDescription")
    private boolean exposeSelfDescription = true;

    @JsonProperty(SETTINGS_PREFIX + "defaultAccessPolicyPath")
    private String defaultAccessPolicyPath;

    @JsonProperty(SETTINGS_PREFIX + "defaultContractPolicyPath")
    private String defaultContractPolicyPath;

    public static synchronized Configuration getInstance() {
        if (Objects.isNull(instance)) {
            instance = new Configuration();
        }
        return instance;
    }

    public URL getRemoteAasLocation() {
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

    public boolean isOnlySubmodels() {
        return onlySubmodels;
    }

    public void setOnlySubmodels(boolean onlySubmodels) {
        this.onlySubmodels = onlySubmodels;
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

}
