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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.net.URL;
import java.util.Objects;

/**
 * Singleton class.
 * The configuration of the application.
 */
@JsonSerialize
public class Configuration {

    private static final String SETTINGS_PREFIX = "edc.aas.";

    private static Configuration instance;

    private boolean initialized;
    private URL remoteAasLocation;
    private String localAasModelPath;
    private int localAasServicePort;
    private String logPrefix = "AAS Extension";
    private String aasServiceConfigPath;
    private URL registryUrl;
    private int syncPeriod = 5; // Seconds
    private boolean exposeSelfDescription = true;
    private String defaultAccessPolicyPath;
    private String defaultContractPolicyPath;
    private int waitForTransferTimeout = 10; // Seconds
    private int waitForAgreementTimeout = 10; // Seconds

    public static synchronized Configuration getInstance() {
        if (Objects.isNull(instance)) {
            instance = new Configuration();
        }
        return instance;
    }

    public URL getRemoteAasLocation() {
        return remoteAasLocation;
    }

    @JsonProperty(SETTINGS_PREFIX + "remoteaaslocation")
    public void setRemoteAasLocation(URL remoteAasLocation) {
        this.remoteAasLocation = remoteAasLocation;
    }

    public String getLocalAasModelPath() {
        return localAasModelPath;
    }

    @JsonProperty(SETTINGS_PREFIX + "localaasmodelpath")
    public void setLocalAasModelPath(String localAasModelPath) {
        this.localAasModelPath = localAasModelPath;
    }

    public boolean isInitialized() {
        return initialized;
    }

    @JsonProperty(SETTINGS_PREFIX + "initialized")
    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public String getLogPrefix() {
        return logPrefix;
    }

    @JsonProperty(SETTINGS_PREFIX + "logprefix")
    public void setLogPrefix(String logPrefix) {
        this.logPrefix = logPrefix;
    }

    public int getLocalAasServicePort() {
        return localAasServicePort;
    }

    @JsonProperty(SETTINGS_PREFIX + "localaasserviceport")
    public void setLocalAasServicePort(int localAasServicePort) {
        this.localAasServicePort = localAasServicePort;
    }

    public String getAasServiceConfigPath() {
        return aasServiceConfigPath;
    }

    @JsonProperty(SETTINGS_PREFIX + "localaasserviceconfigpath")
    public void setAasServiceConfigPath(String aasServiceConfigPath) {
        this.aasServiceConfigPath = aasServiceConfigPath;
    }

    public URL getRegistryUrl() {
        return registryUrl;
    }

    @JsonProperty(SETTINGS_PREFIX + "registryurl")
    public void setRegistryUrl(URL registryUrl) {
        this.registryUrl = registryUrl;
    }

    public int getSyncPeriod() {
        return syncPeriod;
    }

    @JsonProperty(SETTINGS_PREFIX + "syncperiod")
    public void setSyncPeriod(int syncPeriod) {
        this.syncPeriod = syncPeriod;
    }

    public boolean isExposeSelfDescription() {
        return exposeSelfDescription;
    }

    @JsonProperty(SETTINGS_PREFIX + "exposeselfdescription")
    public void setExposeSelfDescription(boolean exposeSelfDescription) {
        this.exposeSelfDescription = exposeSelfDescription;
    }

    public String getDefaultAccessPolicyPath() {
        return defaultAccessPolicyPath;
    }

    @JsonProperty(SETTINGS_PREFIX + "defaultaccesspolicypath")
    public void setDefaultAccessPolicyPath(String defaultAccessPolicyPath) {
        this.defaultAccessPolicyPath = defaultAccessPolicyPath;
    }

    public String getDefaultContractPolicyPath() {
        return defaultContractPolicyPath;
    }

    @JsonProperty(SETTINGS_PREFIX + "defaultcontractpolicypath")
    public void setDefaultContractPolicyPath(String defaultContractPolicyPath) {
        this.defaultContractPolicyPath = defaultContractPolicyPath;
    }

    public int getWaitForAgreementTimeout() {
        return waitForAgreementTimeout;
    }
    
    @JsonProperty(SETTINGS_PREFIX + "client.waitforagreementtimeout")
    public void setWaitForAgreementTimeout(int waitForAgreementTimeout) {
        this.waitForAgreementTimeout = waitForAgreementTimeout;
    }
    public int getWaitForTransferTimeout() {
        return waitForTransferTimeout;
    }
    @JsonProperty(SETTINGS_PREFIX + "client.waitfortransfertimeout")
    public void setWaitForTransferTimeout(int waitForTransferTimeout) {
        this.waitForTransferTimeout = waitForTransferTimeout;
    }
}
